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

package com.liferay.headless.foundation.dto.v1_0;

import com.liferay.petra.function.UnsafeSupplier;

import javax.annotation.Generated;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Javier Gamarra
 * @generated
 */
@Generated("")
@XmlRootElement(name = "PostalAddress")
public class PostalAddress {

	public String getAddressCountry() {
		return _addressCountry;
	}

	public String getAddressLocality() {
		return _addressLocality;
	}

	public String getAddressRegion() {
		return _addressRegion;
	}

	public String getAddressType() {
		return _addressType;
	}

	public Long getId() {
		return _id;
	}

	public String getPostalCode() {
		return _postalCode;
	}

	public String getStreetAddressLine1() {
		return _streetAddressLine1;
	}

	public String getStreetAddressLine2() {
		return _streetAddressLine2;
	}

	public String getStreetAddressLine3() {
		return _streetAddressLine3;
	}

	public void setAddressCountry(String addressCountry) {
		_addressCountry = addressCountry;
	}

	public void setAddressCountry(
		UnsafeSupplier<String, Throwable> addressCountryUnsafeSupplier) {

		try {
			_addressCountry = addressCountryUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setAddressLocality(String addressLocality) {
		_addressLocality = addressLocality;
	}

	public void setAddressLocality(
		UnsafeSupplier<String, Throwable> addressLocalityUnsafeSupplier) {

		try {
			_addressLocality = addressLocalityUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setAddressRegion(String addressRegion) {
		_addressRegion = addressRegion;
	}

	public void setAddressRegion(
		UnsafeSupplier<String, Throwable> addressRegionUnsafeSupplier) {

		try {
			_addressRegion = addressRegionUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setAddressType(String addressType) {
		_addressType = addressType;
	}

	public void setAddressType(
		UnsafeSupplier<String, Throwable> addressTypeUnsafeSupplier) {

		try {
			_addressType = addressTypeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setId(Long id) {
		_id = id;
	}

	public void setId(UnsafeSupplier<Long, Throwable> idUnsafeSupplier) {
		try {
			_id = idUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setPostalCode(String postalCode) {
		_postalCode = postalCode;
	}

	public void setPostalCode(
		UnsafeSupplier<String, Throwable> postalCodeUnsafeSupplier) {

		try {
			_postalCode = postalCodeUnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setStreetAddressLine1(String streetAddressLine1) {
		_streetAddressLine1 = streetAddressLine1;
	}

	public void setStreetAddressLine1(
		UnsafeSupplier<String, Throwable> streetAddressLine1UnsafeSupplier) {

		try {
			_streetAddressLine1 = streetAddressLine1UnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setStreetAddressLine2(String streetAddressLine2) {
		_streetAddressLine2 = streetAddressLine2;
	}

	public void setStreetAddressLine2(
		UnsafeSupplier<String, Throwable> streetAddressLine2UnsafeSupplier) {

		try {
			_streetAddressLine2 = streetAddressLine2UnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	public void setStreetAddressLine3(String streetAddressLine3) {
		_streetAddressLine3 = streetAddressLine3;
	}

	public void setStreetAddressLine3(
		UnsafeSupplier<String, Throwable> streetAddressLine3UnsafeSupplier) {

		try {
			_streetAddressLine3 = streetAddressLine3UnsafeSupplier.get();
	}
		catch (Throwable t) {
			throw new RuntimeException(t);
	}
	}

	private String _addressCountry;
	private String _addressLocality;
	private String _addressRegion;
	private String _addressType;
	private Long _id;
	private String _postalCode;
	private String _streetAddressLine1;
	private String _streetAddressLine2;
	private String _streetAddressLine3;

}