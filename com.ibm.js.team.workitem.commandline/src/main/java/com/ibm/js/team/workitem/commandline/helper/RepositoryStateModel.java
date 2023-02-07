/*******************************************************************************
 * Copyright (c) 2019-2022 IBM
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 * 
 *******************************************************************************/
package com.ibm.js.team.workitem.commandline.helper;

import org.w3c.dom.Document;

import com.ibm.team.repository.common.IAuditableHandle;
import com.ibm.team.workitem.common.model.IWorkItem;

public class RepositoryStateModel {
	private boolean fIsCurrentState;
	private String fStateId;
	private String fPredecessorStateId;
	private String fItemContent;
	private IAuditableHandle fStateHandle;
	private IWorkItem fWorkItem;
	private String fWorkItemId;
	private Document fStateDocument;


	public RepositoryStateModel(String stateId, IAuditableHandle stateHandle) {
		fStateId = stateId;
		fStateHandle = stateHandle;
		if (fStateHandle != null) {

		}
	}

	public IWorkItem getWorkItem() {
		return fWorkItem;
	}

	public void setWorkItem(IWorkItem workItem) {
		this.fWorkItem = workItem;
	}

	public String getStateId() {
		return fStateId;
	}

	public String getItemContent() {
		return fItemContent;
	}

	public String getPredecessorStateId() {
		return fPredecessorStateId;
	}

	public boolean getIsCurrentState() {
		return fIsCurrentState;
	}

	public void setItemContent(String content) {
		fItemContent = content;
	}

	public void setStateId(String id) {
		fStateId = id;
	}

	public void setPredecessorStateId(String id) {
		fPredecessorStateId = id;
	}

	public void setIsCurrentState(boolean isCurrent) {
		fIsCurrentState = isCurrent;
	}

	public Document getStateDocument() {
		return fStateDocument;
	}

	public void setStateDocument(Document fStateDocument) {
		this.fStateDocument = fStateDocument;
	}

	public String getWorkItemId() {
		return fWorkItemId;
	}

	public void setWorkItemId(String fWorkItemId) {
		this.fWorkItemId = fWorkItemId;
	}

}
