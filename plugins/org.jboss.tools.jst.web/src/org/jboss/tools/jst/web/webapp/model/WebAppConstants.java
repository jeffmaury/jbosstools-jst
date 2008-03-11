/*******************************************************************************
 * Copyright (c) 2007 Exadel, Inc. and Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Exadel, Inc. and Red Hat, Inc. - initial API and implementation
 ******************************************************************************/ 
package org.jboss.tools.jst.web.webapp.model;

public interface WebAppConstants {
    public String DOC_QUALIFIEDNAME = "web-app";
    public String DOC_PUBLICID      = "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    public String DOC_PUBLICID_2_3  = "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    public String DOC_EXTDTD        = "http://java.sun.com/j2ee/dtds/web-app_2_2.dtd";

	public String ERROR_CODE = "error-code";
	public String EXCEPTION_TYPE = "exception-type";
	public String SERVLET_CLASS = "servlet-class";
	public String JSP_FILE = "jsp-file";
	public String SERVLET_NAME = "servlet-name";
	public String URL_PATTERN = "url-pattern";
}

