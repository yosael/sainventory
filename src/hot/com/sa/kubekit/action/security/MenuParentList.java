package com.sa.kubekit.action.security;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import com.sa.kubekit.action.util.KubeQuery;
import com.sa.model.security.Menu;

@Name("menuParentList")
@Scope(ScopeType.CONVERSATION)
public class MenuParentList extends KubeQuery<Menu>{

	@Create
	public void init() {
		setJpql("select m from Menu m where m.menuPadre is null order by m.orden");
	}
}
