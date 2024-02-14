/*******************************************************************************
 * Copyright (c) 2019-2022 IBM
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.links.common.IReference;
import com.ibm.team.links.common.registry.IEndPointDescriptor;
// import com.ibm.team.links.common.registry.ILinkTypeRegistry;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.WorkItemEndPoints;

/**
 * Class helps with accessing OSLC Link
 * 
 */
public class WorkItemLocalLinkHelper {
	// public static final IEndPointDescriptor CHANGE_SET = ILinkTypeRegistry.INSTANCE.getLinkType("com.ibm.team.scm.ChangeSet").getTargetEndPointDescriptor();

	private static final List<IEndPointDescriptor> CHECKED_END_POINTS= Arrays.asList(WorkItemEndPoints.PARENT_WORK_ITEM, WorkItemEndPoints.CHILD_WORK_ITEMS, WorkItemEndPoints.BLOCKS_WORK_ITEM, WorkItemEndPoints.DEPENDS_ON_WORK_ITEM, WorkItemEndPoints.COPIED_FROM_WORK_ITEM,
			WorkItemEndPoints.COPIED_WORK_ITEM, WorkItemEndPoints.ATTACHMENT, WorkItemEndPoints.DUPLICATE_WORK_ITEM, WorkItemEndPoints.DUPLICATE_OF_WORK_ITEM, WorkItemEndPoints.RELATED_WORK_ITEM);
	
	public Logger fTracingLog;
//	private String fWorkItemId;
//	private IProgressMonitor fMonitor;
//	private ITeamRepository fTeamRepository;
	public WorkItemLocalLinkHelper(ITeamRepository fTeamRepository, String workItemId, IProgressMonitor fMonitor) {
		super();
//		this.fWorkItemId= workItemId;
//		this.fMonitor = fMonitor;
//		this.fTeamRepository = fTeamRepository;
	}
	
	public final Logger getTracingLog() {
//		return LogFactory.getLog("com.ibm.team.workitem.verify.backlinks.log");
		return fTracingLog;
	}
	public void setTracingLog(Logger log) {
		fTracingLog= log;
	}

//	public void validateLinks(IWorkingCopy workingCopy) {
//		fTracingLog.trace("Validating liks for work item " + fWorkItemId);
//		
//		IWorkItemReferences wiReferences = workingCopy.getReferences();
//		List<IEndPointDescriptor> endPointTypes= wiReferences.getTypes();
//		
//		for (IEndPointDescriptor endPointDescriptor : endPointTypes) {
//			List<IReference> refs = wiReferences.getReferences(endPointDescriptor);
//			for (IReference ref : refs) {
//				if (ref.isItemReference()) {
//					IItemHandle referencedItemHandle= ((IItemReference) ref).getReferencedItem();
//					try  {						
//						IWorkItem item = WorkItemUtil.resolveWorkItem((IWorkItemHandle) referencedItemHandle, IWorkItem.SMALL_PROFILE,
//								getWorkItemCommon(repo),monitor);
//						item.getWorkingCopy().getRequestedStateId();
//						// not deleted
//					} catch (Exception e) {
//						// deleted?
//					}
//				}
//			}
//			
//		}
//		
//	}
	
	public List<IReference> getAllReferences(ITeamRepository teamRepository, Integer workItemId) throws TeamRepositoryException {			
		IWorkItemCommon workItemCommon= (IWorkItemCommon)teamRepository.getClientLibrary(IWorkItemCommon.class);
//		IWorkItem targetWorkItem= workItemCommon.findWorkItemById(workItemId.intValue(), IWorkItem.FULL_PROFILE, null);
		IWorkItem workItem= workItemCommon.findWorkItemById(workItemId, IWorkItem.FULL_PROFILE, null);
		
		List<IReference> allReferenceLists= new ArrayList<IReference>();
		for (IEndPointDescriptor endPoint : CHECKED_END_POINTS) {
			List<IReference> references= workItemCommon.resolveWorkItemReferences(workItem, null).getReferences(endPoint);	
			if (references != null && references.size() > 0 ) {
				allReferenceLists.addAll(references);
			}
		}
		
		return allReferenceLists;
	}

	
	 /* Get WorkItemCommon
	 * 
	 * @return the IWorkItemCommon
	 */
	protected IWorkItemCommon getWorkItemCommon(ITeamRepository repo) {
		IWorkItemCommon workItemCommon = (IWorkItemCommon) repo.getClientLibrary(IWorkItemCommon.class);
		return workItemCommon;
	}
	
	/* From WorkItemPostDeleteTask */ 
//	private void deleteWorkItemLinks(ILinkClosureService linkClosureService, ILinkServiceLibrary linkService, IWorkItemHandle handle) { // TODO: Not yet implemented
//		ILinkServiceLibrary linkService= (ILinkServiceLibrary)getLinkService().getServiceLibrary(ILinkServiceLibrary.class); 
////		try {
////			IItemReference reference= IReferenceFactory.INSTANCE.createReferenceToItem(handle);
////			ILinkQueryPage linksPage= linkService.findLinks(reference);
////			for (ILink cur : linksPage.getAllLinksFromHereOn()) {
////				if (!cur.getLinkType().isConstrained() || WorkItemLinkTypes.isValidLinkType(cur.getLinkType().getLinkTypeId())) {
////					linkService.deleteLink(cur);
////				}
////				if (ILinkClosureService.SUPPORTED_LINK_TYPES.contains(cur.getLinkTypeId())) {
////					IItemHandle srcItem= getItemHandle(cur.getSourceRef());
////					IItemHandle tgtItem= getItemHandle(cur.getTargetRef());
////					if (srcItem != null && tgtItem != null) {
////						linkClosureService.removeRelation(cur.getLinkTypeId(), srcItem.getItemId(), tgtItem.getItemId());
////					}
////				}
////			}
////		} catch (TeamRepositoryException ex) {
////			getTracingLog().warn("Exception deleting work item links: " +  ex);
////		}
////	}
//
//	private IItemHandle getItemHandle(IReference ref) {
//		if (ref != null && ref.isItemReference()) {
//			return ((IItemReference)ref).getReferencedItem();
//		}
//		return null;
//	}
//	private ILinkClosureService getLinkClosureService() {
//		return getService(ILinkClosureService.class);
//	}
//	private ILinkService getLinkService() {
//		return getService(ILinkService.class);
//	}
}
