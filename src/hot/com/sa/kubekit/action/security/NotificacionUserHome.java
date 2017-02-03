package com.sa.kubekit.action.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import com.sa.kubekit.action.util.KubeDAO;
import com.sa.model.inventory.Inventario;
import com.sa.model.security.Usuario;

@Name("notificacionUserHome")
@Scope(ScopeType.CONVERSATION)
public class NotificacionUserHome extends KubeDAO<Usuario>{

	private static final long serialVersionUID = 1L;
	private boolean shofNotif = false;
	public Integer conteo1;
	private HashMap<String, Object> dtRp = new HashMap<String, Object>();
	
	@In
	private LoginUser loginUser;
	
	@Override
	public void create() {
		setCreatedMessage(createValueExpression(sainv_messages
				.get("notifuser_created")));
		setUpdatedMessage(createValueExpression(sainv_messages
				.get("notifuser_updated")));
		setDeletedMessage(createValueExpression(sainv_messages
				.get("notifuser_deleted")));
	}
	
	public void notificacionesInicio() {
		if(loginUser.getUser() != null && loginUser.getUser().isNotificacionInv()) {
			//Sacamos la lista de items que estan abajo del limite de existencias
			String hql = "SELECT x FROM Inventario x WHERE 1 = 1 " +
					"	AND x.producto.cantidadMinima > x.cantidadActual " +
					"	AND x.sucursal = :suc  ORDER BY x.producto.categoria.codigo ASC, " +
					"	x.producto.referencia ASC ";
				
			List<Inventario> inventarios = getEntityManager().createQuery(hql)
					.setParameter("suc", loginUser.getUser().getSucursal())
					.getResultList();
			if(inventarios == null)
				inventarios = new ArrayList<Inventario>();
			
			dtRp.put("prdExisLim", inventarios);
		}
	}
	
	
	
	@Override
	public boolean preSave() {
		return true;
	}

	@Override
	public boolean preModify() {
		return true;
	}
	
	@Override
	public boolean preDelete() {
		return false;
	}

	@Override
	public void posSave() {
		// TODO Auto-generated method stub
	}

	@Override
	public void posModify() {
		// TODO Auto-generated method stub
	}

	@Override
	public void posDelete() {
		// TODO Auto-generated method stub
	}

	
	public boolean isShofNotif() {
		return shofNotif;
	}

	public void setShofNotif(boolean shofNotif) {
		this.shofNotif = shofNotif;
	}

	public Integer getConteo1() {
		return conteo1;
	}

	public void setConteo1(Integer conteo1) {
		this.conteo1 = conteo1;
	}

	public HashMap<String, Object> getDtRp() {
		return dtRp;
	}

	public void setDtRp(HashMap<String, Object> dtRp) {
		this.dtRp = dtRp;
	}

	
	
}
