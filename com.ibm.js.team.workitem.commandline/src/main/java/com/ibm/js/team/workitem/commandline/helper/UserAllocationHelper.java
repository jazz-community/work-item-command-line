/*******************************************************************************
 * Copyright (c) 2019-2022 IBM
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.icu.util.Calendar;
import com.ibm.js.team.workitem.commandline.parameter.ParameterList;
import com.ibm.team.apt.common.resource.IResourcePlanningService;
import com.ibm.team.apt.common.resource.IWorkResourceDetails;
import com.ibm.team.apt.internal.common.resource.dto.DTO_ContributorInfo;
import com.ibm.team.apt.internal.common.resource.model.WorkResourceDetails;
import com.ibm.team.apt.internal.common.rest.resource.dto.OperationReportDTO;
import com.ibm.team.apt.internal.common.util.Dates;
import com.ibm.team.process.common.IDevelopmentLineHandle;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.repository.common.IContributor;
import com.ibm.team.repository.common.IContributorHandle;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.repository.common.UUID;
import com.ibm.team.repository.common.json.JSONObject;
import com.ibm.team.repository.common.model.ItemType;
import com.ibm.team.repository.common.transport.IParameterWrapper;
import com.ibm.team.tpt.internal.common.rest.dto.AllocationBucket;
import com.ibm.team.tpt.internal.common.rest.dto.AvailabilitySearchResultDTO;
import com.ibm.team.tpt.internal.common.rest.dto.ContributorAvailabilityDetails;
import com.ibm.team.tpt.internal.common.rest.dto.RestFactory;

	public class UserAllocationHelper extends WorkItemUpdateHelper {
		IProjectArea projectArea;
		private Logger logger;
		public UserAllocationHelper() {
	}

	public UserAllocationHelper(IProjectArea projectArea, ITeamRepository repository, ParameterList parameters, IProgressMonitor monitor, Logger logger) {
		super();
		this.monitor = monitor;
		this.projectArea= projectArea;
		this.fTeamRepository = repository;
		this.logger= logger;

		if (parameters != null) {
			this.fParameters = parameters;
		}
//		setOverallocationPolicy(parameters
//				.hasSwitch(IAllocationCommandLineConstants.SWITCH_OVER_ALLOCATION_POLICY)); // TODO: over allocation policy
	}

	public String repairAllocations(IContributorHandle contributorHandle) throws TeamRepositoryException {
		String result= null;
		IContributor contributor = (IContributor)  getTeamRepository().itemManager().fetchCompleteItem(contributorHandle, 0, this.monitor);
		contributor.getDetails();
		final IResourcePlanningService resourceService= getResourcePlanningService();
		
		Calendar startDate= Calendar.getInstance();
		startDate.set(2010, 9, 4);
		Calendar endDate= Calendar.getInstance();
		endDate.set(3000, 9, 8); // Y3K problem
		// TODO: Fetch a list of contributors, but not so many that it uses up memory
		IContributor[] contributorArrray= {  contributor }; 
		final DTO_ContributorInfo[] resourceInfoDTOs= resourceService.fetchContributorInfo(contributorArrray, new Timestamp(startDate.getTimeInMillis()), new Timestamp(endDate.getTimeInMillis()), true);
		
		if (resourceInfoDTOs != null && resourceInfoDTOs.length > 0) { // should be 1 item
			for (int i=0; i< resourceInfoDTOs.length;i++) {				
				String repairResult= repairAllocation(contributor, resourceInfoDTOs[i], resourceService);
				if (repairResult != null) {
					result+= repairResult + "\n";
				}
			}
		}
		
//		AvailabilitySearchResultDTO searchResult= fResourceAllocationRestService.postSearchUsersByAvailability(args);
//        List availabilityDetails= searchResult.getAvailabilityDetails();
//		ContributorAvailabilityDetails cad= (ContributorAvailabilityDetails) availabilityDetails.get(0);
		
//		List allocationBuckets= cad.getAllocationDetails();
//		AllocationBucket ab= (AllocationBucket) allocationBuckets.get(0);
//		ab.getHoursAvailable();
//		ab.getStartDate();
//		ab.getEndDate();
		
		
		// Return either null for success or a message for the command line result.
		return result;
	}
	
	// TODO: This can be optimized by repairing more than one at a time
	private String repairAllocation(IContributor contributor, DTO_ContributorInfo resourceInfoDTO, IResourcePlanningService resourceService) throws TeamRepositoryException {
		String result= null; // success
		@SuppressWarnings("unchecked")
		List<IWorkResourceDetails> workDetails= resourceInfoDTO.getWorkDetails();
		ArrayList<IWorkResourceDetails> workResourceDetails= new ArrayList<IWorkResourceDetails>();
		boolean isChanged;
		for(IWorkResourceDetails workDetail : workDetails) {
			isChanged= repairWorkDetail(workDetail);
			if(isChanged) {
				workResourceDetails.add(workDetail);
			}
		}
		// check result then uncomment this.
//		resourceService.saveWorkDetails((IWorkResourceDetails[])workResourceDetails.toArray());		
		return result;
	}

public OperationReportDTO saveWorkAssignments(List<AllocationDTOParser> parsedAllocations, DTO_ContributorInfo info) throws TeamRepositoryException {
		
		@SuppressWarnings("unchecked")
		List<WorkResourceDetails> workDetails = (List<WorkResourceDetails>)info.getWorkDetails();
		List<WorkResourceDetails> workingCopies= new ArrayList<WorkResourceDetails>();
		
		HashMap<String, IWorkResourceDetails> details= new HashMap<String, IWorkResourceDetails>();
		for (Object object : workDetails) {
			IWorkResourceDetails workDetail= (IWorkResourceDetails) object;
			details.put(workDetail.getItemId().getUuidValue(), workDetail);
		}
		
		for (AllocationDTOParser allocationParser : parsedAllocations) {
			IWorkResourceDetails workDetail = allocationParser.prepareWorkDetail(details, workDetails);
			if(workDetail != null){
				workingCopies.add((WorkResourceDetails) workDetail);
			}
		}
		
		if (workingCopies.isEmpty())
			return null;
		
		IWorkResourceDetails[] workingCopiesArray= workingCopies.toArray(new WorkResourceDetails[workingCopies.size()]);
		
//		try{
//			fSavedAllocations= fPlanningService.saveWorkDetails(workingCopiesArray);
//		}catch (TeamRepositoryException e) {
//			if (e.getData() != null && e.getData() instanceof OperationReportDTO) {
//				return (OperationReportDTO) e.getData();				
//			}
//			throw e;
//		}		
		
		return null;
	}
	
	
	private boolean repairWorkDetail(IWorkResourceDetails workDetail) {
		if (workDetail.isNewItem()) {
	//		workDetail.
			return true;
		}
		return false;
	}
	
	private IResourcePlanningService getResourcePlanningService() {
		return (IResourcePlanningService) getTeamRepository().getClientLibrary(
				IResourcePlanningService.class);
	}
	
	private ContributorAvailabilityDetails createContributorAvailabilityDetails(
			final DTO_ContributorInfo contributorDTO, Timestamp finishDate, List<AllocationBucket> allocationBuckets)
			throws TeamRepositoryException {
//		IContributor contributor = resolveItem(contributorDTO.getContributor());
//		
//		String stateId = contributor.getStateId().getUuidValue();
//		String itemId = contributor.getItemId().getUuidValue();
//		String contribHandle = UIItemHandle.stateHandleFrom(ItemType.Contributor, itemId, stateId).getHandleValue();
//		
//		ContributorAvailabilityDetails cad = RestFactory.eINSTANCE.createContributorAvailabilityDetails();
//		cad.setContributorUserId(contributor.getUserId());
//		cad.setContributorItemId(itemId);
//		cad.setContributorHandle(contribHandle);
//		cad.setContributorName(contributor.getName());
//		cad.setFinishDate(finishDate);
//		
//		cad.getAllocationDetails().addAll(allocationBuckets);
//		return cad;
		return null;
	}
	
	private static final class ParmsSearchUsers implements IParameterWrapper {
		public String[] contributorIds;
		public String hoursRequired;
		public String endTime;
		public String startTime;
	}
	
	private static final class WorkAllocationResult implements IParameterWrapper {
		public String contributorId;
		public String message= null;
		public int    errorLevel= 0; // (0= normal, 1= warning, 2= error)
		public boolean isUpdated= false;
		public IWorkResourceDetails updatedResourceDetails; // (0= normal, 1= warning, 2= error)
	}
	
	public class AllocationDTOParser{
		
		private static final String PARAM_ITEM_ID= "itemId"; //$NON-NLS-1$
		private static final String PARAM_START_DATE= "startDate"; //$NON-NLS-1$
		private static final String PARAM_END_DATE= "endDate"; //$NON-NLS-1$
		private static final String PARAM_CONTRIBUTOR_ID = "contributorId"; //$NON-NLS-1$
		private static final String PARAM_OWNER_ID = "ownerId"; //$NON-NLS-1$
		private static final String PARAM_LINE_ID = "lineId"; //$NON-NLS-1$
		private static final String PARAM_IS_NEW_ITEM = "isNewItem"; //$NON-NLS-1$
		private static final String PARAM_PERCENTAGE = "percentage"; //$NON-NLS-1$
		
		private static final String EMPTY_STRING = ""; //$NON-NLS-1$
		
		private static final String DATE_PATTERN= "yyyy-MM-dd"; //$NON-NLS-1$
		private final SimpleDateFormat DATE_PARSER= new SimpleDateFormat(DATE_PATTERN, Locale.ENGLISH);
		
		private String fAllocationDTO = null;
		private String id = EMPTY_STRING;
		private Long percentage;
		private Timestamp startDate;
		private Timestamp endDate;
		private Boolean isNew;
		private String lineId;
		private String ownerId;
		private String contributorId;
		
		public AllocationDTOParser(String allocationDTO){
			fAllocationDTO = allocationDTO;
			
		}
		
		public void parse() throws IllegalArgumentException{
			try {
				JSONObject allocationJSON = JSONObject.parse(new StringReader(fAllocationDTO));
			
				id= (String) allocationJSON.get(PARAM_ITEM_ID);
				if(id == null){
					id = EMPTY_STRING;
				}
				percentage = (Long) allocationJSON.get(PARAM_PERCENTAGE);				
				isNew= (Boolean) allocationJSON.get(PARAM_IS_NEW_ITEM);
				lineId= (String) allocationJSON.get(PARAM_LINE_ID);
				ownerId= (String) allocationJSON.get(PARAM_OWNER_ID);
				contributorId = (String) allocationJSON.get(PARAM_CONTRIBUTOR_ID);
				
				String startDateStr= (String) allocationJSON.get(PARAM_START_DATE);
				String endDateStr= (String) allocationJSON.get(PARAM_END_DATE);				
				startDate= parse2(startDateStr);
				endDate= parse2(endDateStr);
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			} catch (ParseException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public IWorkResourceDetails prepareWorkDetail(HashMap<String, IWorkResourceDetails> details, List workDetails) {
			IWorkResourceDetails workDetail = null;
			if (isNew) {
				List<IWorkResourceDetails> matchedWorkDetails = findWorkDetail(lineId, ownerId, workDetails);
				if(!matchedWorkDetails.isEmpty() && matchedWorkDetails.size() > 0){
					workDetail = matchedWorkDetails.get(0);
				}
				// now you can have two assignments to the same dev line and same owner
				// with different assignment dates
				if ((null != workDetail)) {
					WorkResourceDetails workingCopy= (WorkResourceDetails) IWorkResourceDetails.ITEM_TYPE.createItem();
					workingCopy.setOwner(workDetail.getOwner());
					workingCopy.setDevelopmentLine(workDetail.getDevelopmentLine());
					workingCopy.setContributor(workDetail.getContributor());
					if(!id.equals(EMPTY_STRING) && matchedWorkDetails.size() == 1 && workDetail.isNewItem()){
						//if there is only one allocation for this time line and process area and that is also calculated
						// then only set the item. This is required to remove this from the unchanged. 
						UUID calculatedAllocationUUID = UUID.valueOf(id);
						((WorkResourceDetails)workDetail).setItemId(calculatedAllocationUUID);
						workingCopy.setItemId(calculatedAllocationUUID);
					}
					workingCopy.setCustomized(false);
					workDetail= workingCopy;
				}
			} else {
				workDetail= details.get(id);
			}
			if (workDetail != null) {
				if (workDetail.getAssignment() != percentage.intValue() || Dates.compareTo2(workDetail.getStartDate(), startDate) != 0 ||
						Dates.compareTo2(workDetail.getEndDate(), endDate) != 0 || isNew) { 
					// save even if the user has not explicitly updated the values otherwise the validation calculation causes confusions						
					
					WorkResourceDetails workingCopy= (WorkResourceDetails) workDetail.getWorkingCopy();
					workingCopy.setAssignment(percentage.intValue());
					
					if (null != startDate)
					  workingCopy.setStartDate(startDate);
					if (null != endDate)
					  workingCopy.setEndDate(endDate);
					return workingCopy;
				}
			}
			return null;
		}
		
		private List<IWorkResourceDetails> findWorkDetail(String lineId, String ownerId, List workDetails) {
			List<IWorkResourceDetails> matchedAllocations = new ArrayList<IWorkResourceDetails>();
			for (Object object : workDetails) {
				IWorkResourceDetails detail= (IWorkResourceDetails) object;
				IAuditableHandle owner= detail.getOwner();
				IDevelopmentLineHandle line= detail.getDevelopmentLine();
				if (owner != null && line != null && owner.getItemId().getUuidValue().equals(ownerId) && line.getItemId().getUuidValue().equals(lineId))
					matchedAllocations.add(detail);
			}
			
			return matchedAllocations;
		}
		
		private  Timestamp parse(String isoString) throws ParseException {
			Calendar calendar= Calendar.getInstance(); 
			// see defect 95651
			calendar.setTimeInMillis(DATE_PARSER.parse(isoString).getTime());
			Dates.setGMTHighNoon(calendar);
			return Dates.toTimestamp(calendar.getTime());
		}
		
		private  Timestamp parse2(String isoString) throws ParseException {
			return (null != isoString && !"".equals(isoString)) ? parse(isoString) : null;//$NON-NLS-1$ 
		}

		public String getContributorId() {
			return contributorId;
		}
		
		public String getOwner() {
			return ownerId;
		}
	}

}
