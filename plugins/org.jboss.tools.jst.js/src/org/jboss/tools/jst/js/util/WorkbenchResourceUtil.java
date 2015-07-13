/*******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *       Red Hat, Inc. - initial API and implementation
 *******************************************************************************/
package org.jboss.tools.jst.js.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.tools.jst.js.internal.Activator;

/**
 * @author Ilya Buziuk (ibuziuk)
 */
public final class WorkbenchResourceUtil {

	private WorkbenchResourceUtil() {
	}

	public static void openInEditor(final IFile file) throws PartInitException {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
				try {
					page.openEditor(new FileEditorInput(file), desc.getId());
				} catch (PartInitException e) {
					Activator.logError(e);
				}
			}
		});

	}

	public static void createFile(final IFile file, final String content) throws CoreException {
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (!file.exists()) {
					InputStream source = new ByteArrayInputStream(content.getBytes());
					try {
						file.create(source, IResource.NONE, null);
					} catch (CoreException e) {
						Activator.logError(e);
					}
				}
			}
		});
	}
	
	public static void updateFile(IFile file, String content) throws CoreException {
		if (file.exists()) {
			InputStream source = new ByteArrayInputStream(content.getBytes());
			file.setContents(source, true, true, new NullProgressMonitor());
		}
	}

	public static IProject getSelectedProject() {
		IWorkbenchWindow workbenchWindow = Activator.getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (workbenchWindow != null) {
			IWorkbenchPage activePage = workbenchWindow.getActivePage();
			if (activePage != null) {
				ISelection selection = activePage.getSelection();

				if (selection instanceof StructuredSelection) {
					StructuredSelection structuredSelection = (StructuredSelection) selection;
					Object firstElement = structuredSelection.getFirstElement();
					IResource resource = ResourceUtil.getResource(firstElement);
					if (resource != null) {
						return resource.getProject();
					}
				}
			}
		}
		return null;
	}

	public static IProject getProject(String projectString) {
		if (projectString != null) {
			try {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectString);
				if (project != null && project.exists()) {
					return project;
				}
			} catch (IllegalArgumentException e) {
			}
		}

		return null;
	}
	
	public static IContainer getContainerFromSelection(IStructuredSelection selection) {
		IContainer container = null;
		if (selection != null && !selection.isEmpty()) {
			Object selectedObject = selection.getFirstElement();
			if (selectedObject instanceof IContainer) {
				container = (IContainer) selectedObject;
			} else if (selectedObject instanceof IFile) {
				container = ((IFile) selectedObject).getParent();
			}
		}
		return container;
	}
	
	public static String getAbsolutePath(IResource resource) {
		IPath path = null;
		String absoluteLocation = null;
		if (resource != null) {
			path = resource.getRawLocation();
			path = (path != null) ? path : resource.getLocation();
			if (path != null) {
				absoluteLocation = path.toOSString();
			}
		}
		return absoluteLocation;
	}
	

}