package com.sa.kubekit.action.sales;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;

import com.sa.kubekit.action.security.LoginUser;
import com.sa.kubekit.action.util.KubeDAO;
import com.sa.model.inventory.Categoria;
import com.sa.model.inventory.Producto;
import com.sa.model.sales.ComboAparato;
import com.sa.model.sales.CostoServicio;
import com.sa.model.sales.CotizacionComboApa;
import com.sa.model.sales.CotizacionComboItem;
import com.sa.model.sales.ItemComboApa;
import com.sa.model.sales.Service;

@Name("comboAparatoHome")
@Scope(ScopeType.CONVERSATION)
public class ComboAparatoHome extends KubeDAO<ComboAparato>{

	private static final long serialVersionUID = 1L;
	private Integer cmbApaId;	
	private List<ComboAparato> resultList = new ArrayList<ComboAparato>();
	private List<CostoServicio> costos = new ArrayList<CostoServicio>();
	private List<ItemComboApa> items = new ArrayList<ItemComboApa>();
	private Float totalCostos;
	private Float totalItems;
	private boolean tieneGarantia;
	private String nomCoinci="";
	private String filterEstado;

	/*
	@In(required=false,create=true)
	private CategoriaHome categoriaHome;
	
	@In(required=false,create=true)
	private ProductoHome productoHome;
		*/
	@Override
	public void create() {
		setCreatedMessage(createValueExpression(sainv_messages
				.get("combapa_created")));
		setUpdatedMessage(createValueExpression(sainv_messages
				.get("combapa_updated")));
		setDeletedMessage(createValueExpression(sainv_messages
				.get("combapa_deleted")));
	}
	
	@In
	private LoginUser loginUser;
	
	public void load(){
		try{
			setInstance(getEntityManager().find(ComboAparato.class, cmbApaId));
			//Cargamos lista de items y costos
			items = instance.getItemsCombo();
			costos = instance.getCostosCombo();
			if(instance.getPeriodoGarantia() != null && instance.getPeriodoGarantia() > 0)
				setTieneGarantia(true);
			else
				setTieneGarantia(false);
			
		}catch (Exception e) { 
			clearInstance();
			setInstance(new ComboAparato());
		}
	}
	
	
		
	public void getCombosList() {
		setInstance(new ComboAparato());
		resultList = getEntityManager().createQuery("SELECT c FROM ComboAparato c ORDER BY c.codigo ASC ").getResultList();
	}
	
	public void cargarCombosActivos() {
		
		setInstance(new ComboAparato());
		resultList = getEntityManager().createQuery("SELECT c FROM ComboAparato c WHERE c.estado = 'ACT' ORDER BY c.codigo ASC ").getResultList();
		
	}
	
	//funcion que retorna resultados seg�n contenido del buscador en tiempo real.
	public void buscadorCombos(){
		
		resultList=getEntityManager().createQuery("SELECT c FROM ComboAparato c " +
				"WHERE (UPPER(c.nombre) LIKE UPPER(:nom) OR UPPER(c.codigo) LIKE UPPER(:cod)) ORDER BY c.estado ASC,c.codigo ASC ")
				.setParameter("cod","%"+nomCoinci.toUpperCase()+"%" )
				.setParameter("nom","%"+nomCoinci.toUpperCase()+"%")
				.getResultList();
		
	}
	
	public void buscadorCombosAct(){
			
			resultList=getEntityManager().createQuery("SELECT c FROM ComboAparato c " +
					"WHERE (UPPER(c.nombre) LIKE UPPER(:nom) OR UPPER(c.codigo) LIKE UPPER(:cod)) and c.estado='ACT' ORDER BY c.codigo ASC ")
					.setParameter("cod","%"+nomCoinci.toUpperCase()+"%" )
					.setParameter("nom","%"+nomCoinci.toUpperCase()+"%")
					.getResultList();
			
		}
	
	public void actualizarPrecio(ItemComboApa selItm) {
		
		
	}
	
	public void setPrincipal(ItemComboApa selItm) {
		for(ItemComboApa tmpItm : items) 
			tmpItm.setPrincipal(false);
		selItm.setPrincipal(true);
	}

	public void agregarCosto(Service pv) {
		boolean existe = false;
		if(!existe) {
			CostoServicio nwCst = new CostoServicio();
			nwCst.setServicio(pv);
			nwCst.setValor(pv.getCosto().floatValue());
			costos.add(nwCst);
		}
	}
	
	public void agregarCategoria(Categoria ct) {
		boolean existe = false;
		for(ItemComboApa tmpItm : items)
			if(tmpItm.getCategoria() != null && tmpItm.getCategoria().equals(ct) && tmpItm.getProducto() == null) {
				existe = true; break;
			}
		
		if(!existe) {
			ItemComboApa nwItm = new ItemComboApa();
			nwItm.setCategoria(ct);
			nwItm.setCantidad(Short.valueOf("1"));
			items.add(nwItm);
		}
	}
	
	public void agregarItem(Producto pr) {
		
		boolean existe = false;
		for(ItemComboApa tmpItm : items)
			if(tmpItm.getProducto() != null && tmpItm.getProducto().equals(pr)) {
				existe = true; break;
			}
				
		if(!existe) {
			ItemComboApa nwItm = new ItemComboApa();
			nwItm.setProducto(pr);
			nwItm.setCategoria(pr.getCategoria());
			nwItm.setCantidad(Short.valueOf("1"));
			if(items.size() <= 0)
				nwItm.setPrincipal(true);
			items.add(nwItm);
		}
	}
	
	public void delItem(ItemComboApa itm) {
		items.remove(itm);
	}
	
	public void delCosto(CostoServicio cst) {
		costos.remove(cst);
	}
	
	@Override
	public boolean preSave() {
		if(!validar())
			return false;
		
		if(!isTieneGarantia())
			instance.setPeriodoGarantia(0);
			
		instance.setEmpresa(loginUser.getUser().getSucursal().getEmpresa());
		
		return true;
	}
	
	private boolean validar(){
		if(costos == null || costos.size() <= 0) {
		} else {
			for(CostoServicio tmpCst : costos) 
				if(tmpCst.getValor() == null || (tmpCst.getValor() <= 0 )) {
					FacesMessages.instance().add(
							sainv_messages.get("combapa_error_valowcst"));
					return false;
				}
		}
		boolean hayPrincipal = false;
		if(items == null || items.size() <= 0) {
			FacesMessages.instance().add(
					sainv_messages.get("combapa_error_noitm"));
			return false;
		} else {
			for(ItemComboApa tmpItm : items) {
				if(tmpItm.getCantidad() == null || tmpItm.getCantidad() <= 0) {
					FacesMessages.instance().add(
							sainv_messages.get("combapa_error_valowitm"));
					return false;
				}
				if(tmpItm.isPrincipal())
					hayPrincipal = true;
			}
			if(!hayPrincipal) {
				FacesMessages.instance().add(
						sainv_messages.get("combapa_error_noprpal"));
				return false;
			}
				
		}
		
		//Evaluamos el codigo del combo que no se repita
		List<ComboAparato> combosCoinci = new ArrayList<ComboAparato>();
		if(!isManaged()) { 
			combosCoinci = getEntityManager()
					.createQuery("SELECT c FROM ComboAparato c WHERE c.codigo = :cod AND c.estado='ACT' ")
					.setParameter("cod", instance.getCodigo())
					.getResultList();
			
		} else {
			combosCoinci = getEntityManager()
					.createQuery("SELECT c FROM ComboAparato c WHERE c.codigo = :cod AND c.id <> :idCmb AND c.estado='ACT'")
					.setParameter("cod", instance.getCodigo())
					.setParameter("idCmb", instance.getId())
					.getResultList();
		}
		
		if(combosCoinci != null && combosCoinci.size() > 0) {
			sainv_messages.clear();
			FacesMessages.instance().add(
					sainv_messages.get("combapa_error_codrep"));
			return false;
		}
		
		return true;
	}
	
	private boolean guardarDetalleCombo() {
		//Guardamos el detalle de costos e items
		getEntityManager().refresh(instance);
		
		for(CostoServicio tmpCst : costos) 
			if(!instance.getCostosCombo().contains(tmpCst)) {
				tmpCst.setCombo(instance);
				getEntityManager().persist(tmpCst);
			} else
				getEntityManager().merge(tmpCst);
			
		for(ItemComboApa tmpItm : items) 
			if(!instance.getItemsCombo().contains(tmpItm)) {
				tmpItm.setCombo(instance);
				getEntityManager().persist(tmpItm);
			} else
				getEntityManager().merge(tmpItm);
		
		//Si estamos actualizando y no insertando, verificamos si eliminaron alguno
		if(instance.getId() != null && instance.getId() > 0) {
			for(CostoServicio tmpCst2 : instance.getCostosCombo()) 
				if(!costos.contains(tmpCst2)) 
					getEntityManager().remove(tmpCst2);
			
			for(ItemComboApa tmpItm2 : instance.getItemsCombo()) 
				if(!items.contains(tmpItm2)) 
					getEntityManager().remove(tmpItm2);
		}
		
		getEntityManager().flush();
		return true;
	}

	@Override
	public boolean preModify() {
		if(!validar())
			return false;
		
		if(!isTieneGarantia())
			instance.setPeriodoGarantia(0);
		
		return true;
	}

	@Override
	public boolean preDelete() {
		//Borramos las cotizaciones a las que esta amarrado un combo y a sus detalles
		if(instance.getItemsCombo() != null)
		for(ItemComboApa itm : instance.getItemsCombo()) 
			getEntityManager().remove(itm);
		if(instance.getCostosCombo() != null)
		for(CostoServicio cst : instance.getCostosCombo()) 
			getEntityManager().remove(cst);
		
		List<CotizacionComboApa> lstCots = getEntityManager()
				.createQuery("SELECT c FROM CotizacionComboApa c WHERE c.combo = :cmb")
				.setParameter("cmb", instance)
				.getResultList();
		
		if(lstCots != null)
		for(CotizacionComboApa tmpCot : lstCots) {
			for(CotizacionComboItem tmpItmCot : tmpCot.getItemsCotizacion())
				getEntityManager().remove(tmpItmCot);
			if(tmpCot.getHijoBin() != null && tmpCot.getHijoBin().size() > 0)
				getEntityManager().remove(tmpCot.getHijoBin().get(0));
			getEntityManager().remove(tmpCot);
		}
		getEntityManager().refresh(instance);
		
		return true;
	}

	@Override
	public void posSave() {
		guardarDetalleCombo();
		
	}

	@Override
	public void posModify() {
		guardarDetalleCombo();
	}

	@Override
	public void posDelete() {
		// TODO Auto-generated method stub
		
	}

	public Integer getCmbApaId() {
		return cmbApaId;
	}

	public void setCmbApaId(Integer cmbApaId) {
		this.cmbApaId = cmbApaId;
	}

	public List<ComboAparato> getResultList() {
		return resultList;
	}

	public void setResultList(List<ComboAparato> resultList) {
		this.resultList = resultList;
	}

	public List<CostoServicio> getCostos() {
		return costos;
	}

	public void setCostos(List<CostoServicio> costos) {
		this.costos = costos;
	}

	public List<ItemComboApa> getItems() {
		return items;
	}

	public void setItems(List<ItemComboApa> items) {
		this.items = items;
	}

	public Float getTotalCostos() {
		return totalCostos;
	}

	public void setTotalCostos(Float totalCostos) {
		this.totalCostos = totalCostos;
	}

	public Float getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(Float totalItems) {
		this.totalItems = totalItems;
	}

	public boolean isTieneGarantia() {
		return tieneGarantia;
	}

	public void setTieneGarantia(boolean tieneGarantia) {
		this.tieneGarantia = tieneGarantia;
	}
	public String getNomCoinci() {
		return nomCoinci;
	}



	public void setNomCoinci(String nomCoinci) {
		this.nomCoinci = nomCoinci;
	}



	public String getFilterEstado() {
		return filterEstado;
	}



	public void setFilterEstado(String filterEstado) {
		this.filterEstado = filterEstado;
	}
	
	
	
}
