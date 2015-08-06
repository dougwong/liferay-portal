/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.configuration.persistence;

import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBFactoryUtil;
import com.liferay.portal.kernel.io.ReaderInputStream;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayOutputStream;
import com.liferay.portal.kernel.util.ReflectionUtil;

import java.io.IOException;
import java.io.StringReader;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.sql.DataSource;

import org.apache.felix.cm.NotCachablePersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.file.ConfigurationHandler;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Raymond Augé
 */
@Component(
	immediate = true,
	property = {
		Constants.SERVICE_RANKING + ":Integer=" + (Integer.MAX_VALUE - 1000)
	},
	service = {PersistenceManager.class, ReloadablePersitenceManager.class}
)
public class ConfigurationPersistenceManager
	implements NotCachablePersistenceManager, ReloadablePersitenceManager {

	@Override
	public void delete(final String pid) throws IOException {
		if (System.getSecurityManager() != null) {
			try {
				AccessController.doPrivileged(
					new PrivilegedExceptionAction<Void>() {

						@Override
						public Void run() throws Exception {
							_delete(pid);

							return null;
						}

					});
			}
			catch (PrivilegedActionException pae) {
				throw (IOException)pae.getException();
			}
		}
		else {
			_delete(pid);
		}
	}

	@Override
	public boolean exists(String pid) {
		ReadLock readLock = _readWriteLock.readLock();

		try {
			readLock.lock();

			return _dictionaryMap.containsKey(pid);
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public Enumeration<?> getDictionaries() {
		ReadLock readLock = _readWriteLock.readLock();

		try {
			readLock.lock();

			return Collections.enumeration(_dictionaryMap.values());
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public Dictionary<?, ?> load(String pid) {
		ReadLock readLock = _readWriteLock.readLock();

		try {
			readLock.lock();

			return _dictionaryMap.get(pid);
		}
		finally {
			readLock.unlock();
		}
	}

	@Override
	public void reload(String pid) throws IOException {
		WriteLock writeLock = _readWriteLock.writeLock();

		try {
			writeLock.lock();

			_dictionaryMap.remove(pid);

			if (hasPid(pid)) {
				Dictionary<?, ?> dictionary = loadFromDB(pid);

				_dictionaryMap.put(pid, dictionary);
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	@Override
	public void store(
			final String pid,
			@SuppressWarnings("rawtypes") final Dictionary dictionary)
		throws IOException {

		if (System.getSecurityManager() != null) {
			try {
				AccessController.doPrivileged(
					new PrivilegedExceptionAction<Void>() {

						@Override
						public Void run() throws Exception {
							_store(pid, dictionary);

							return null;
						}

					});
			}
			catch (PrivilegedActionException pae) {
				throw (IOException)pae.getException();
			}
		}
		else {
			_store(pid, dictionary);
		}
	}

	@Activate
	protected void activate() {
		ReadLock readLock = _readWriteLock.readLock();
		WriteLock writeLock = _readWriteLock.writeLock();

		try {
			readLock.lock();

			if (!hasConfigurationTable()) {
				readLock.unlock();
				writeLock.lock();

				try {
					createConfigurationTable();
				}
				finally {
					readLock.lock();
					writeLock.unlock();
				}
			}

			loadAllRecords();
		}
		finally {
			readLock.unlock();
		}
	}

	protected String buildSQL(String sql) throws IOException {
		DB db = DBFactoryUtil.getDB();

		return db.buildSQL(sql);
	}

	protected void cleanUp(
		Connection connection, Statement statement, ResultSet resultSet) {

		try {
			if (resultSet != null) {
				resultSet.close();
			}
		}
		catch (SQLException sqle) {
			ReflectionUtil.throwException(sqle);
		}
		finally {
			try {
				if (statement != null) {
					statement.close();
				}
			}
			catch (SQLException sqle) {
				ReflectionUtil.throwException(sqle);
			}
			finally {
				try {
					if (connection != null) {
						connection.close();
					}
				}
				catch (SQLException sqle) {
					ReflectionUtil.throwException(sqle);
				}
			}
		}
	}

	protected boolean hasConfigurationTable() {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		try {
			connection = _dataSource.getConnection();

			preparedStatement = connection.prepareStatement(
				buildSQL(_TEST_CONFIGURATION_TABLE_EXISTS));

			resultSet = preparedStatement.executeQuery();

			int count = 0;

			if (resultSet.next()) {
				count = resultSet.getInt(1);
			}

			if (count >= 0) {
				return true;
			}

			return false;
		}
		catch (IOException | SQLException e) {
			return false;
		}
		finally {
			cleanUp(connection, preparedStatement, resultSet);
		}
	}

	protected void createConfigurationTable() {
		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;

		try {
			connection = _dataSource.getConnection();

			statement = connection.createStatement();

			statement.executeUpdate(buildSQL(_TABLE_SQL_CREATE));
		}
		catch (IOException | SQLException e) {
			ReflectionUtil.throwException(e);
		}
		finally {
			cleanUp(connection, statement, resultSet);
		}
	}

	@Deactivate
	protected void deactivate() {
		_dictionaryMap.clear();
	}

	protected void deleteFromDatabase(String pid) throws IOException {
		Connection connection = null;
		PreparedStatement preparedStatement = null;

		try {
			connection = _dataSource.getConnection();

			preparedStatement = connection.prepareStatement(
				buildSQL(_DELETE_CONFIGURATION_SQL));

			preparedStatement.setString(1, pid);

			preparedStatement.executeUpdate();
		}
		catch (SQLException se) {
			throw new IOException(se);
		}
		finally {
			cleanUp(connection, preparedStatement, null);
		}
	}

	protected boolean hasPid(String pid) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 0;

		try {
			con = _dataSource.getConnection();

			ps = con.prepareStatement(buildSQL(_COUNT_CONFIGURATION_SQL));

			ps.setString(1, pid);

			rs = ps.executeQuery();

			if (rs.next()) {
				count = rs.getInt(1);
			}

			if (count > 0) {
				return true;
			}

			return false;
		}
		catch (IOException | SQLException se) {
			return ReflectionUtil.throwException(se);
		}
		finally {
			cleanUp(con, ps, rs);
		}
	}

	protected void loadAllRecords() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = _dataSource.getConnection();

			ps = con.prepareStatement(
				buildSQL(_RETRIEVE_ALL_CONFIGURATION_SQL),
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			rs = ps.executeQuery();

			while (rs.next()) {
				String pid = rs.getString(1);
				String configuration = rs.getString(2);

				_dictionaryMap.putIfAbsent(pid, read(configuration));
			}
		}
		catch (IOException | SQLException e) {
			ReflectionUtil.throwException(e);
		}
		finally {
			cleanUp(con, ps, rs);
		}
	}

	protected Dictionary<?, ?> loadFromDB(String pid) throws IOException {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = _dataSource.getConnection();

			ps = con.prepareStatement(buildSQL(_RETRIEVE_CONFIGURATION_SQL));

			ps.setString(1, pid);

			rs = ps.executeQuery();

			if (rs.next()) {
				return read(rs.getString(1));
			}

			return _EMPTY_DICTIONARY;
		}
		catch (SQLException se) {
			return ReflectionUtil.throwException(se);
		}
		finally {
			cleanUp(con, ps, rs);
		}
	}

	protected Dictionary<?, ?> read(String configuration)
		throws IOException {

		ReaderInputStream readerInputStream = new ReaderInputStream(
			new StringReader(configuration));

		return ConfigurationHandler.read(readerInputStream);
	}

	@Reference(target = "(bean.id=liferayDataSource)")
	protected void setDataSource(DataSource dataSource) {
		_dataSource = dataSource;
	}

	protected void store(ResultSet resultSet, Dictionary<?, ?> dictionary)
		throws IOException, SQLException {

		UnsyncByteArrayOutputStream outputStream =
			new UnsyncByteArrayOutputStream();

		ConfigurationHandler.write(outputStream, dictionary);

		resultSet.updateString(2, outputStream.toString());
	}

	@SuppressWarnings("resource")
	protected void storeInDB(String pid, Dictionary<?, ?> dictionary)
		throws IOException {

		UnsyncByteArrayOutputStream outputStream =
			new UnsyncByteArrayOutputStream();

		ConfigurationHandler.write(outputStream, dictionary);

		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;

		try {
			con = _dataSource.getConnection();
			con.setAutoCommit(false);

			ps = con.prepareStatement(
				buildSQL(_RETRIEVE_CONFIGURATION_SQL_FOR_UPDATE),
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);

			ps.setString(1, pid);

			rs = ps.executeQuery();

			if (rs.next()) {
				rs.updateString(2, outputStream.toString());
				rs.updateRow();
			}
			else {
				ps = con.prepareStatement(
					buildSQL(_INSERT_INTO_CONFIGURATION_SQL));

				ps.setString(1, pid);
				ps.setString(2, outputStream.toString());
				ps.executeUpdate();
			}

			con.commit();
		}
		catch (SQLException se) {
			ReflectionUtil.throwException(se);
		}
		finally {
			cleanUp(con, ps, rs);

			outputStream.close();
		}
	}

	private void _delete(String pid) throws IOException {
		WriteLock writeLock = _readWriteLock.writeLock();

		try {
			writeLock.lock();

			Dictionary<?, ?> dictionary = _dictionaryMap.remove(pid);

			if ((dictionary != null) && hasPid(pid)) {
				deleteFromDatabase(pid);
			}
		}
		finally {
			writeLock.unlock();
		}
	}

	private void _store(
			String pid, @SuppressWarnings("rawtypes") Dictionary dictionary)
		throws IOException {

		WriteLock writeLock = _readWriteLock.writeLock();

		try {
			writeLock.lock();

			storeInDB(pid, dictionary);

			_dictionaryMap.put(pid, dictionary);
		}
		finally {
			writeLock.unlock();
		}
	}

	private static final String _COUNT_CONFIGURATION_SQL =
		"select count(*) from Configuration_ where configurationId = ?";

	private static final String _DELETE_CONFIGURATION_SQL =
		"delete from Configuration_ where configurationId = ?";

	private static final Dictionary<?, ?> _EMPTY_DICTIONARY = new Hashtable<>();

	private static final String _INSERT_INTO_CONFIGURATION_SQL =
		"insert into Configuration_ (configurationId, dictionary) values " +
			"(?, ?)";

	private static final String _RETRIEVE_ALL_CONFIGURATION_SQL =
		"select configurationId, dictionary from Configuration_ ORDER BY " +
			"configurationId ASC";

	private static final String _RETRIEVE_CONFIGURATION_SQL =
		"select dictionary from Configuration_ where configurationId = ?";

	private static final String _RETRIEVE_CONFIGURATION_SQL_FOR_UPDATE =
		"select configurationId, dictionary from Configuration_ where " +
			"configurationId = ?";

	private static final String _TABLE_SQL_CREATE =
		"create table Configuration_ (configurationId VARCHAR(255) not null " +
			"primary key, dictionary TEXT)";

	private static final String _TEST_CONFIGURATION_TABLE_EXISTS =
		"select count(*) from Configuration_";

	private DataSource _dataSource;
	private final ConcurrentMap<String, Dictionary<?, ?>> _dictionaryMap =
		new ConcurrentHashMap<>();
	private final ReentrantReadWriteLock _readWriteLock =
		new ReentrantReadWriteLock(true);

}