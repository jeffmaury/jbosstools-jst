/******************************************************************************* 
 * Copyright (c) 2014 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.jst.web.ui.internal.preferences.js;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.jboss.tools.common.xml.XMLUtilities;
import org.jboss.tools.jst.web.html.HTMLConstants;
import org.jboss.tools.jst.web.kb.WebKbPlugin;
import org.jboss.tools.jst.web.kb.internal.JSRecognizer;
import org.jboss.tools.jst.web.kb.internal.taglib.html.jq.JQueryMobileVersion;
import org.jboss.tools.jst.web.kb.taglib.IHTMLLibraryVersion;
import org.jboss.tools.jst.web.ui.WebUiPlugin;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 
 * @author Viacheslav Kabanovich
 *
 */
public class PreferredJSLibVersions implements IPreferredJSLibVersion {
	IFile f;
	IHTMLLibraryVersion version;

	private Set<String> disabledLibs = new HashSet<String>();
	private Set<String> enabledLibs = new HashSet<String>();

	private boolean addMetaViewport = true;

	private Map<String, Boolean> preferredLibs = new HashMap<String, Boolean>();
	private Map<String, String> preferredVersions = new HashMap<String, String>();

	/**
	 * Variable to save/load library version selections in current project.
	 */
	static Map<Object, QualifiedName> PREFERRED_LIBS = new HashMap<Object, QualifiedName>(); 
	
	static QualifiedName getQualifiedName(Object v) {
		QualifiedName result = PREFERRED_LIBS.get(v);
		if(result == null) {
			result = new QualifiedName(WebUiPlugin.PLUGIN_ID, "preferred-js-libs-" + v.toString());
			PREFERRED_LIBS.put(v, result);
		}
		return result;
	}

	public PreferredJSLibVersions(IFile file, IHTMLLibraryVersion version) {
		f = file;
		this.version = version;
	}

	/**
	 * Returns true, if lib is already implemented in html.
	 * @param libName
	 * @return
	 */
	public boolean isLibDisabled(String libName) {
		return disabledLibs.contains(libName);
	}

	public boolean areAllLibsDisabled() {
		return enabledLibs.isEmpty();
	}

	/**
	 * Returns true if lib is selected.
	 * @param libName
	 * @return
	 */
	public boolean shouldAddLib(String libName) {
		return preferredLibs.containsKey(libName) && preferredLibs.get(libName);
	}

	/**
	 * Returns version selected for the library.
	 * @param libName
	 * @return
	 */
	public String getLibVersion(String libName) {
		return preferredVersions.get(libName);
	}

	/**
	 * For all js libraries in preferences computes
	 * 1. If it is already referenced in html, then it is marked as disabled;
	 * 2. Creates default selection and preferred version.
	 * 3. Loads from project previously saved selection and preferred version, if available.
	 */
	public void updateLibEnablementAndSelection() {
		JSLibModel model = JSLibFactory.getInstance().getPreferenceModel();
		for (String libName: new ArrayList<String>(preferredLibs.keySet())) {
			JSLib lib = model.getLib(libName);
			if(lib == null || lib.getVersions().isEmpty()) {
				enabledLibs.remove(libName);
				disabledLibs.remove(libName);
				preferredLibs.remove(libName);
				preferredVersions.remove(libName);
			}
		}
		for(JSLib lib: model.getLibs()) {
			if(lib.getVersions().isEmpty()) continue;
			String libName = lib.getName();
			boolean enabled = true;
			if(version.isPreferredJSLib(null, libName)) {
				enabled = f == null || !version.isReferencingJSLib(f, libName);
			} else {
				String libNameRoot = getLibNameRoot(lib);
				if(libNameRoot != null) {
					enabled = f == null || JSRecognizer.getJSReferenceVersion(f, libNameRoot) == null;
				}
			}
			if(enabled) {
				enabledLibs.add(libName);
			} else  {
				disabledLibs.add(libName);
			}
		}
		Set<String> availableLibs = new HashSet<String>();
		for(JSLib lib: model.getLibs()) {
			if(lib.getVersions().isEmpty()) continue;
			String libName = lib.getName();
			Boolean current = preferredLibs.get(libName);
			String currentVersion = getLibVersion(libName);
			if(currentVersion != null && lib.getVersion(currentVersion) == null) {
				currentVersion = null;
			}
			String mask = null;
			boolean add = version.isPreferredJSLib(f, libName);

			if(libName.equals(JQueryMobileVersion.JQ_CATEGORY)) {
				mask = version == JQueryMobileVersion.JQM_1_3 ? "1.9." : "2.0.";
			} else if(libName.equals(JQueryMobileVersion.JQM_CATEGORY)) {
				mask = version == JQueryMobileVersion.JQM_1_3 ? "1.3." : "1.4.";
			}
			if(current == null) {
				preferredLibs.put(libName, add);
				if(!add && version.isPreferredJSLib(null, libName)) {
					//as for Ionic to prevent default value for project.
					availableLibs.add(libName);
				}
			}
			if(currentVersion == null) {
				String lastVersion = getLastVersion(lib, mask);
				if(lastVersion == null) {
					preferredLibs.put(libName, Boolean.FALSE);
					current = null;
					String[] ns = lib.getVersionNames().toArray(new String[0]);
					lastVersion = ns[ns.length - 1];
				}
				preferredVersions.put(libName, lastVersion);
			}
			if(current != null) {
				availableLibs.add(libName);
			}
		}
		String pl = null;
		try {
			if(f != null) {
				pl = f.getProject().getPersistentProperty(getQualifiedName(version));
			}
		} catch (CoreException e) {
			WebUiPlugin.getDefault().logError(e);
		}
		if(pl != null) {
			StringTokenizer st = new StringTokenizer(pl, ";");
			while(st.hasMoreTokens()) {
				String t = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(t, ":");
				if(st2.countTokens() == 3) {
					String name = st2.nextToken();
					if(preferredLibs.containsKey(name) && !availableLibs.contains(name)) {
						boolean add = "true".equals(st2.nextToken());
						String version = st2.nextToken();
						if(model.getLib(name).getVersion(version) != null) {
							preferredVersions.put(name, version);
							preferredLibs.put(name, add);
						} else {
							if(!add) {
								preferredLibs.put(name, add);
							}
						}
					}
				}
			}
		}

		addMetaViewport = !containsMetaViewport(f);
	}

	private String getLastVersion(JSLib lib, String mask) {
		String[] ns = lib.getVersionNames().toArray(new String[0]); 
		if(mask != null) {
			for (int i = ns.length - 1; i >= 0; i--) {
				if(ns[i].startsWith(mask)) {
					return ns[i];
				}
			}
		} else {
			return ns[ns.length - 1];
		}
		return null;
	}

	private String getLibNameRoot(JSLib lib) {
		for (JSLibVersion v: lib.getVersions()) {
			for (String u: v.getURLs()) {
				int i = u.lastIndexOf('/') + 1;
				int j = u.indexOf('-', i);
				if(j >= 0) {
					return u.substring(i, j + 1);
				}
			}
		}
		return null;
	}

	/**
	 * Stores library version preferences in maps.
	 */
	public void applyLibPreference(IPreferredJSLibVersion preferredVersions) {
		for(JSLib lib: JSLibFactory.getInstance().getPreferenceModel().getLibs()) {
			if(lib.getVersions().isEmpty()) continue;
			String libName = lib.getName();
			if(!enabledLibs.contains(libName)) continue;
			preferredLibs.put(libName, preferredVersions.shouldAddLib(libName));
			this.preferredVersions.put(libName, preferredVersions.getLibVersion(libName));
		}
	}

	/**
	 * Stores library version preferences in current project.
	 */
	public void saveLibPreference() {
		StringBuffer sb = new StringBuffer();
		for (String libName: preferredLibs.keySet()) {
			String shouldAdd = "" + preferredLibs.get(libName);
			String version = preferredVersions.get(libName);
			sb.append(libName).append(":").append(shouldAdd).append(":").append(version).append(";");
		}
		try {
			if(f != null) {
				f.getProject().setPersistentProperty(getQualifiedName(version), sb.toString());
			}
		} catch (CoreException e) {
			WebUiPlugin.getDefault().logError(e);
		}
	}

	public String[][] getURLs(Node node) {
		Set<String> referencedJS = new TreeSet<String>();
		Set<String> referencedCSS = new TreeSet<String>();
		if(node instanceof Element) {
			Element head = (Element)node;
			for (Element c: XMLUtilities.getChildren(head, HTMLConstants.TAG_LINK)) {
				String href = c.getAttribute(HTMLConstants.ATTR_HREF);
				if(href != null && href.length() > 0) {
					referencedCSS.add(href);
				}
			}
			for (Element c: XMLUtilities.getChildren(head, HTMLConstants.TAG_SCRIPT)) {
				String src = c.getAttribute(HTMLConstants.ATTR_SRC);
				if(src != null && src.length() > 0) {
					referencedJS.add(src);
				}
			}
		}
		JSLibModel model = JSLibFactory.getInstance().getPreferenceModel();
		String[][] result = new String[2][];
		List<String> css = new ArrayList<String>();
		List<String> js = new ArrayList<String>();
		for(JSLib lib: model.getSortedLibs()) {
			String libName = lib.getName();
			if(disabledLibs.contains(libName)) continue;
			if(!preferredLibs.containsKey(libName) || !preferredLibs.get(libName)) continue;
			String preferredVersion = preferredVersions.get(libName);
			JSLibVersion version = lib.getVersion(preferredVersion);
			String[] urls = version.getSortedUrls();
			for (String u: urls) {
				if(version.isCSS(u)) {
					if(referencedCSS.contains(u)) continue;
					//no need to do other checks as the library is enabled.
					css.add(u);
				} else if(version.isJS(u)) {
					if(referencedJS.contains(u)) continue;
					//no need to do other checks as the library is enabled.
					js.add(u);
				}
			}
		}
		result[0] = css.toArray(new String[0]);
		result[1] = js.toArray(new String[0]);
		return result;
	}

	public boolean addMetaViewport() {
		return addMetaViewport;
	}

	static boolean containsMetaViewport(IFile file) {
		if(file == null) return false;
		IStructuredModel model = null;
		try {
			model = StructuredModelManager.getModelManager().getModelForRead(file);
			IDOMDocument xmlDocument = (model instanceof IDOMModel) ? ((IDOMModel) model).getDocument() : null;
			if(xmlDocument != null) {
				Element htmlNode = JSRecognizer.findChildElement(xmlDocument, "html");
				if(htmlNode != null) {
					Element headNode = JSRecognizer.findChildElement(htmlNode, "head");
					if(headNode != null) {
						Element[] metaNodes = JSRecognizer.findChildElements(headNode, "meta");
						for (Element meta : metaNodes) {
							String name = JSRecognizer.getAttribute(meta, "name");
							if("viewport".equals(name)) {
								return true;
							}
						}
					}
				}
			}
		} catch (IOException e) {
			WebKbPlugin.getDefault().logError(e);
		} catch (CoreException e) {
			WebKbPlugin.getDefault().logError(e);
		} finally {
			if (model != null) {
				model.releaseFromRead();
			}
		}
		
		return false;
	}

}
