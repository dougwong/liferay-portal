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

package com.liferay.portal.repository.liferayrepository;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.repository.Repository;
import com.liferay.portal.kernel.repository.capabilities.ProcessorCapability;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.repository.util.RepositoryWrapper;
import com.liferay.portal.service.ServiceContext;

import java.io.File;
import java.io.InputStream;

/**
 * @author Adolfo Pérez
 */
public class LiferayProcessorRepositoryWrapper extends RepositoryWrapper {

	public LiferayProcessorRepositoryWrapper(
		Repository repository, ProcessorCapability processorCapability) {

		super(repository);

		_processorCapability = processorCapability;
	}

	@Override
	public FileEntry addFileEntry(
			long userId, long folderId, String sourceFileName, String mimeType,
			String title, String description, String changeLog, File file,
			ServiceContext serviceContext)
		throws PortalException {

		FileEntry fileEntry = super.addFileEntry(
			userId, folderId, sourceFileName, mimeType, title, description,
			changeLog, file, serviceContext);

		_processorCapability.generateNew(fileEntry);

		return fileEntry;
	}

	@Override
	public FileEntry addFileEntry(
			long userId, long folderId, String sourceFileName, String mimeType,
			String title, String description, String changeLog, InputStream is,
			long size, ServiceContext serviceContext)
		throws PortalException {

		FileEntry fileEntry = super.addFileEntry(
			userId, folderId, sourceFileName, mimeType, title, description,
			changeLog, is, size, serviceContext);

		_processorCapability.generateNew(fileEntry);

		return fileEntry;
	}

	@Override
	public void deleteFileEntry(long fileEntryId) throws PortalException {
		FileEntry fileEntry = getFileEntry(fileEntryId);

		super.deleteFileEntry(fileEntryId);

		_processorCapability.cleanUp(fileEntry);
	}

	@Override
	public void deleteFileEntry(long folderId, String title)
		throws PortalException {

		FileEntry fileEntry = getFileEntry(folderId, title);

		super.deleteFileEntry(folderId, title);

		_processorCapability.cleanUp(fileEntry);
	}

	@Override
	public void deleteFileVersion(long fileEntryId, String version)
		throws PortalException {

		FileEntry fileEntry = getFileEntry(fileEntryId);

		FileVersion fileVersion = fileEntry.getFileVersion(version);

		super.deleteFileVersion(fileEntryId, version);

		_processorCapability.cleanUp(fileVersion);
	}

	private final ProcessorCapability _processorCapability;

}