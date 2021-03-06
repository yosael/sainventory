package com.sa.kubekit.action.inventory;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import com.sa.kubekit.action.security.LoginUser;
import com.sa.kubekit.action.util.KubeQuery;
import com.sa.model.inventory.Pedido;

@Name("pedidosRechazadoList")
@Scope(ScopeType.CONVERSATION)
public class PedidoRechazadoList extends KubeQuery<Pedido>{

	@In
	private LoginUser loginUser;
	
	@Create
	public void init() {
		if(loginUser.getUser().getSucursal()==null){
			setJpql("select e from Pedido e where e.estado like 'R' order by e.fechaInicio desc, e.proveedor.nombre");
		}else{
			setJpql("select e from Pedido e where e.estado like 'R' and e.sucursal.id = " + loginUser.getUser().getSucursal().getId() + 
					" order by e.fechaInicio desc, e.proveedor.nombre");
		}
		
	}
}
