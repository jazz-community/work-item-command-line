/*******************************************************************************
 * Copyright (c) 2015-2017 IBM Corporation
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.js.team.workitem.commandline.framework.WorkItemCommandLineException;
import com.ibm.team.links.common.IReference;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IAttachment;
import com.ibm.team.workitem.common.model.IAttachmentHandle;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.IWorkItemReferences;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;

public class AttachmentUtil {

	/**
	 * Save all attachments of a work item to disk
	 * 
	 * @param workItem - the work item
	 * @param folder   - the folder to save to
	 * @return
	 * @throws TeamRepositoryException
	 */
	public static List<IAttachment> saveAttachmentsToDisk(File folder, IWorkItem workItem,
			IWorkItemCommon workItemCommon, IProgressMonitor monitor) throws TeamRepositoryException {
		List<IAttachment> resultList = new ArrayList<IAttachment>();
		List<IAttachment> allAttachments = AttachmentUtil.findAttachments(workItem, workItemCommon, monitor);
		if (allAttachments.isEmpty()) {
			return resultList;
		}
		FileUtil.createFolderWithParents(folder);
		for (IAttachment anAttachment : allAttachments) {
			resultList.add(AttachmentUtil.saveAttachmentToDisk(anAttachment, folder, monitor));
		}
		return resultList;
	}

	/**
	 * Saves one attachment to disk
	 * 
	 * @param attachment - the attachment to save
	 * @param folder     - the folder name to save into
	 * @throws TeamRepositoryException
	 */
	public static IAttachment saveAttachmentToDisk(IAttachment attachment, File folder, IProgressMonitor monitor)
			throws TeamRepositoryException {
		String attachmentFileName = folder.getAbsolutePath() + File.separator + attachment.getName();
		try {
			int retry = 0;
			File save = new File(attachmentFileName);

			OutputStream out;
			while (true) {
				out = new FileOutputStream(save);
				try {
					((ITeamRepository) attachment.getOrigin()).contentManager().retrieveContent(
							attachment.getContent(), out, monitor);
					return attachment;
				} catch (TeamRepositoryException e) {
					retry++;
					if (retry > 4)
						throw e;
					System.out.println("Retry (" + retry + ") file: " + attachmentFileName);
				} finally {
					out.close();
				}
			}
		} catch (FileNotFoundException e) {
			throw new WorkItemCommandLineException("Attach File - File not found: " + attachmentFileName, e);
		} catch (IOException e) {
			throw new WorkItemCommandLineException("Attach File - I/O Exception: " + attachmentFileName, e);
		}
	}

	/**
	 * Finds all attachments of a workitem
	 * 
	 * @return - a list of the attachments
	 * @throws TeamRepositoryException
	 */
	public static List<IAttachment> findAttachments(IWorkItem workItem, IWorkItemCommon workItemCommon,
			IProgressMonitor monitor) throws TeamRepositoryException {
		List<IAttachment> foundAttachments = new ArrayList<IAttachment>();
		// get all the references
		int retry = 0;
		IWorkItemReferences references = null;
		while (references == null) {
			try {
				references = workItemCommon.resolveWorkItemReferences(workItem, monitor);
			} catch (TeamRepositoryException e) {
				retry++;
				if (retry > 4)
					throw e;
				System.out.println("Retry (" + retry + ") find attachments for: " + workItem.getId());
			}
		}
		// narrow down to the attachments
		List<IReference> attachments = references.getReferences(WorkItemEndPoints.ATTACHMENT);
		for (IReference aReference : attachments) {
			Object resolvedReference = aReference.resolve();
			if (resolvedReference instanceof IAttachmentHandle) {
				IAttachmentHandle handle = (IAttachmentHandle) resolvedReference;
				retry = 0;
				IAttachment attachment = null;
				while (attachment == null) {
					try {
						attachment = workItemCommon.getAuditableCommon().resolveAuditable(handle,
								IAttachment.DEFAULT_PROFILE, monitor);
					} catch (TeamRepositoryException e) {
						retry++;
						if (retry > 4)
							throw e;
						System.out.println("Retry (" + retry + ") find attachment.");
					}
				}
				foundAttachments.add(attachment);
			}
		}
		return foundAttachments;
	}

	/**
	 * Remove all attachments from a work item
	 * 
	 * @throws TeamRepositoryException
	 */
	public static void removeAllAttachments(IWorkItem workItem, IWorkItemCommon workItemCommon, IProgressMonitor monitor)
			throws TeamRepositoryException {
		List<IAttachment> allAttachments = findAttachments(workItem, workItemCommon, monitor);
		for (IAttachment anAttachment : allAttachments) {
			workItemCommon.deleteAttachment(anAttachment, monitor);
		}
	}

	/**
	 * Remove an attachment if the filename and the description are the same
	 * 
	 * @param fileName    - the filename of the attachment
	 * @param description - the description of the attachment
	 * @throws TeamRepositoryException
	 */
	public static void removeAttachment(String fileName, String description, IWorkItem workItem,
			IWorkItemCommon workItemCommon, IProgressMonitor monitor) throws TeamRepositoryException {
		File thisFile = new File(fileName);
		List<IAttachment> allAttachments = findAttachments(workItem, workItemCommon, monitor);
		for (IAttachment anAttachment : allAttachments) {
			if (anAttachment.getName().equals(thisFile.getName()) && anAttachment.getDescription().equals(description)) {
				workItemCommon.deleteAttachment(anAttachment, monitor);
			}
		}
	}

}
