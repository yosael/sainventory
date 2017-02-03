package com.sa.kubekit.action.medical;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import com.sa.kubekit.action.util.KubeQuery;
import com.sa.model.sales.Service;

@Name("serviceQuery")
@Scope(ScopeType.CONVERSATION)
public class ServiceQuery extends KubeQuery<Service> {

	@Create
	public void init() {
		setJpql("select e from Service e order by e.estado ASC,e.codigo ASC");
	}

}