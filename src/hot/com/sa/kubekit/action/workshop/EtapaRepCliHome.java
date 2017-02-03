package com.sa.kubekit.action.workshop;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;

import sun.security.jca.GetInstance;

import com.sa.kubekit.action.security.LoginUser;
import com.sa.kubekit.action.util.KubeDAO;
import com.sa.model.security.Sucursal;
import com.sa.model.workshop.EtapaRepCliente;
import com.sa.model.workshop.ItemRequisicionEta;
import com.sa.model.workshop.ReparacionCliente;
import com.sa.model.workshop.RequisicionEtapaRep;
import com.sa.model.workshop.ServicioReparacion;

@Name("etapaRepCliHome")
@Scope(ScopeType.CONVERSATION)
@SuppressWarnings("unchecked")
public class EtapaRepCliHome extends KubeDAO<EtapaRepCliente> {

	private static final long serialVersionUID = 1L;

	private List<Object[]> etapasRepCli = new ArrayList<Object[]>();
	private List<ItemRequisicionEta> itmsRequi = new ArrayList<ItemRequisicionEta>();

	private Integer etaRepId;
	private String accionEta;
	private String descRechazo;
	private String nomCoinci;
	private boolean noTieneRep;

	@In
	private LoginUser loginUser;

	@In(required = false, create = true)
	@Out(required = false)
	private ReparacionClienteHome reparacionClienteHome;
	
	public void repsPendientes() {
		// Formamos query para mostrar reparaciones con etapas del area de
		// negocio del usuario logueado
		try{
			
			Sucursal sucursalUser=new Sucursal();
			if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null)
			{
				sucursalUser=loginUser.getUser().getSucursal().getSucursalSuperior();
			}
			else
				sucursalUser=loginUser.getUser().getSucursal();
			
			
			 if(loginUser.getUser().getAreaUsuario() == null && getNomCoinci()==null)
			{
				System.out.println("**** Entro al if numero 3 ");
					etapasRepCli = getEntityManager().createNativeQuery(
							"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
									+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
									+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
									+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
									+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
									+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
									+ " etapa_rep_cliente etc "
									+ " WHERE apc.cliente_id = cli.cliente_id " //AND rpc.aprobada = true
									+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
									+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
									+ " and suc.id = rpc.sucursal_id " 
									+ " and etc.estado = 'PEN' "
									+ " ORDER BY rpc.fecha_ingreso ASC ")
					.getResultList();	
					
				
			}
			else if(loginUser.getUser().getAreaUsuario() == null && getNomCoinci()!=null)
			{	
				System.out.println("**** Entro al if numero 4 ");
					etapasRepCli = getEntityManager().createNativeQuery(
							"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
									+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
									+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
									+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
									+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
									+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
									+ " etapa_rep_cliente etc "
									+ " WHERE apc.cliente_id = cli.cliente_id AND (UPPER(cli.nombres) LIKE :nom OR UPPER(cli.apellidos) LIKE :ape)" // AND rpc.aprobada = true
									+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
									+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
									+ " and suc.id = rpc.sucursal_id " 
									+ " and etc.estado = 'PEN' "
									/*+ " 	and etr.orden = (select MIN(tetr.orden) from etapa_reparacion tetr, etapa_rep_cliente tetc "
									+ "   where tetr.etarep_id = tetc.etarep_id and rpc.repcli_id = tetc.repcli_id "
									+ "	and tetr.areneg_id = ? and tetc.estado = 'PEN') " */
									+ " ORDER BY rpc.fecha_ingreso ASC ")
					.setParameter("nom", "%"+getNomCoinci().toUpperCase()+"%")
					.setParameter("ape", "%"+getNomCoinci().toUpperCase()+"%")
					//.setParameter(2, (loginUser.getUser().getAreaUsuario() != null)?loginUser.getUser().getAreaUsuario().getId():0 )
					.getResultList();
			}
			//Para taller + busqueda
			else if (getNomCoinci()!=null && loginUser.getUser().getAreaUsuario().getId() == 3){ //  se agrega validdacion de nomCoinci, variable de filtro (buscador de ordenes de trabajo por nombre de cliente)
				
				System.out.println("**** Entro al if numero 1 taller + busqueda");
				etapasRepCli = getEntityManager().createNativeQuery(
									"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
											+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
											+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
											+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
											+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
											+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
											+ " etapa_rep_cliente etc "
											+ " WHERE apc.cliente_id = cli.cliente_id AND (UPPER(cli.nombres) LIKE :nom OR UPPER(cli.apellidos) LIKE :ape)" // AND rpc.aprobada = true
											+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
											+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
											+ " and suc.id = rpc.sucursal_id and etr.areneg_id = :neg" 
											+ " and etc.estado = 'PEN' "
											/*+ " 	and etr.orden = (select MIN(tetr.orden) from etapa_reparacion tetr, etapa_rep_cliente tetc "
											+ "   where tetr.etarep_id = tetc.etarep_id and rpc.repcli_id = tetc.repcli_id "
											+ "	and tetr.areneg_id = ? and tetc.estado = 'PEN') " */
											+ " ORDER BY rpc.fecha_ingreso ASC ")
							.setParameter("neg", loginUser.getUser().getAreaUsuario().getId())
							.setParameter("nom", "%"+getNomCoinci().toUpperCase()+"%")
							.setParameter("ape", "%"+getNomCoinci().toUpperCase()+"%")
							//.setParameter(2, (loginUser.getUser().getAreaUsuario() != null)?loginUser.getUser().getAreaUsuario().getId():0 )
							.getResultList();
				System.out.println("Size del result list de trabajos: " +etapasRepCli.size());
				
				
			}  //para el area de negocio de taller
			else if (loginUser.getUser().getAreaUsuario().getId() == 3 && getNomCoinci()==null)
			{	
					System.out.println("**** Entro al if numero 1 taller");
					etapasRepCli = getEntityManager().createNativeQuery(
									"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
											+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
											+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
											+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
											+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
											+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
											+ " etapa_rep_cliente etc"
											+ " WHERE apc.cliente_id = cli.cliente_id "
											+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
											+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
											+ " and suc.id = rpc.sucursal_id and etr.areneg_id = :neg" 
											+ " and etc.estado = 'PEN' "
											+ " ORDER BY rpc.fecha_ingreso ASC ")
							.setParameter("neg", loginUser.getUser().getAreaUsuario().getId())
							
							// Al setear la variable del area de negocio como se aprecia abajo da error (desde que se agreg� filtro de busqueda por nombre de cliente)
							//.setParameter(2, (loginUser.getUser().getAreaUsuario() != null)?loginUser.getUser().getAreaUsuario().getId():0 )  
							.getResultList();
				
						
						System.out.println(" *** AREA DE NEGOCIO USUARIO" + loginUser.getUser().getAreaUsuario().getId());
						
					
					
			}// para audiologa
			else if (loginUser.getUser().getAreaUsuario().getId() == 1 && getNomCoinci()==null)
			{	
					System.out.println("**** Entro al if numero 2 audiologa");
					etapasRepCli = getEntityManager().createNativeQuery(
									"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
											+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
											+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
											+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
											+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
											+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
											+ " etapa_rep_cliente etc"
											+ " WHERE apc.cliente_id = cli.cliente_id "
											+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
											+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
											+ " and suc.id = rpc.sucursal_id and etr.areneg_id = :neg" 
											+ " and etc.estado = 'PEN' and rpc.sucursal_id=:sucUser "
											+ " ORDER BY rpc.fecha_ingreso ASC ")
							.setParameter("neg", loginUser.getUser().getAreaUsuario().getId())
							.setParameter("sucUser",sucursalUser.getId())
							
							// Al setear la variable del area de negocio como se aprecia abajo da error (desde que se agreg� filtro de busqueda por nombre de cliente)
							//.setParameter(2, (loginUser.getUser().getAreaUsuario() != null)?loginUser.getUser().getAreaUsuario().getId():0 )  
							.getResultList();
				
						
						System.out.println(" *** AREA DE NEGOCIO USUARIO" + loginUser.getUser().getAreaUsuario().getId());
						
					
					
			}
			// para audiologa + busqueda
			else if (loginUser.getUser().getAreaUsuario().getId() == 1 && getNomCoinci()!=null)
			{	
					System.out.println("**** Entro al if numero 2 audiologa + busqueda");
					etapasRepCli = getEntityManager().createNativeQuery(
									"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
											+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
											+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
											+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
											+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
											+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
											+ " etapa_rep_cliente etc"
											+ " WHERE apc.cliente_id = cli.cliente_id AND (UPPER(cli.nombres) LIKE :nom OR UPPER(cli.apellidos) LIKE :ape)"
											+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
											+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
											+ " and suc.id = rpc.sucursal_id and etr.areneg_id = :neg" 
											+ " and etc.estado = 'PEN' and rpc.sucursal_id=:sucUser "
											+ " ORDER BY rpc.fecha_ingreso ASC ")
							.setParameter("neg", loginUser.getUser().getAreaUsuario().getId())
							.setParameter("sucUser",sucursalUser.getId())
							.setParameter("nom", "%"+getNomCoinci().toUpperCase()+"%")
							.setParameter("ape", "%"+getNomCoinci().toUpperCase()+"%")
							
							// Al setear la variable del area de negocio como se aprecia abajo da error (desde que se agreg� filtro de busqueda por nombre de cliente)
							//.setParameter(2, (loginUser.getUser().getAreaUsuario() != null)?loginUser.getUser().getAreaUsuario().getId():0 )  
							.getResultList();
				
						
						System.out.println(" *** AREA DE NEGOCIO USUARIO" + loginUser.getUser().getAreaUsuario().getId());
						
					
					
			}
			
		} 
		catch (Exception e){
			
			/*if(loginUser.getUser().getAreaUsuario() == null && getNomCoinci()==null)
			{
				System.out.println("**** Entro al if numero 3 ");
					etapasRepCli = getEntityManager().createNativeQuery(
							"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
									+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
									+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
									+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
									+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
									+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
									+ " etapa_rep_cliente etc "
									+ " WHERE apc.cliente_id = cli.cliente_id " //AND rpc.aprobada = true
									+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
									+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
									+ " and suc.id = rpc.sucursal_id " 
									+ " and etc.estado = 'PEN' "
									+ " ORDER BY rpc.fecha_ingreso ASC ")
					.getResultList();	
					
				
			}
			else 
			{	
				System.out.println("**** Entro al if numero 4 ");
					etapasRepCli = getEntityManager().createNativeQuery(
							"SELECT prt.nombre nomProceso, etr.nombre nomEtapa, "
									+ "	cli.nombres || ' ' || cli.apellidos nomCliente, apc.nombre nomProducto,"
									+ "	rpc.fecha_ingreso fechaEstFin, etc.fecha_real_fin fechaReaFin, etc.etarepcli_id id,"
									+ "	prt.codigo codProceso, rpc.repcli_id idRep, suc.nombre nomSucursal" 
									+ " FROM aparato_cliente apc, cliente cli, sucursal suc,"
									+ " reparacion_cliente rpc, etapa_reparacion etr, proceso_taller prt, "
									+ " etapa_rep_cliente etc "
									+ " WHERE apc.cliente_id = cli.cliente_id AND (UPPER(cli.nombres) LIKE :nom OR UPPER(cli.apellidos) LIKE :ape)" // AND rpc.aprobada = true
									+ " and rpc.apacli_id = apc.apacli_id and rpc.repcli_id = etc.repcli_id "
									+ " and etr.prctll_id = prt.prctll_id and etr.etarep_id = etc.etarep_id "
									+ " and suc.id = rpc.sucursal_id " 
									+ " and etc.estado = 'PEN' "
									+ " 	and etr.orden = (select MIN(tetr.orden) from etapa_reparacion tetr, etapa_rep_cliente tetc "
									+ "   where tetr.etarep_id = tetc.etarep_id and rpc.repcli_id = tetc.repcli_id "
									+ "	and tetr.areneg_id = ? and tetc.estado = 'PEN') " 
									+ " ORDER BY rpc.fecha_ingreso ASC ")
					.setParameter("nom", "%"+getNomCoinci().toUpperCase()+"%")
					.setParameter("ape", "%"+getNomCoinci().toUpperCase()+"%")
					//.setParameter(2, (loginUser.getUser().getAreaUsuario() != null)?loginUser.getUser().getAreaUsuario().getId():0 )
					.getResultList();
			}*/
			
			e.printStackTrace();
		}
		
	}

	public void load() {
		try {
			setInstance((EtapaRepCliente) getEntityManager()
					.createQuery(
							"select e from EtapaRepCliente e where e.id = :id")
					.setParameter("id", etaRepId).getSingleResult());
			reparacionClienteHome.setRepCliId(instance.getReparacionCli()
					.getId());
			reparacionClienteHome.load();
		} catch (Exception e) {
			clearInstance();
			setInstance(new EtapaRepCliente());

		}
	}

	public void cargarEtapasRep() {
		etapasRepCli = getEntityManager()
				.createQuery(
						"SELECT e FROM EtapaRepCliente e "
								+ "	WHERE e.reparacionCli = :rep ORDER BY e.etapaRep.orden ASC")
				.setParameter("rep", this.reparacionClienteHome.getInstance())
				.getResultList();
		// Cargamos las requisiciones en una sola lista
		itmsRequi = getEntityManager()
				.createQuery(
						"SELECT i FROM ItemRequisicionEta i WHERE i.reqEtapa.etapaRepCli.reparacionCli = :rep")
				.setParameter("rep", reparacionClienteHome.getInstance())
				.getResultList();

		Double totSrv = 0d;
		Double totItm = 0d;

		if (reparacionClienteHome.getInstance().getServiciosRep() != null)
			for (ServicioReparacion tmpServ : reparacionClienteHome
					.getInstance().getServiciosRep())
				totSrv += tmpServ.getServicio().getCosto();

		if (itmsRequi != null)
			for (ItemRequisicionEta tmpIR : itmsRequi)
				totItm += tmpIR.getProducto().getPrcNormal()
						* tmpIR.getCantidad();

		reparacionClienteHome.setTotalServs(totSrv);
		reparacionClienteHome.setTotalItems(totItm);
	}

	public String calcTiempoRespuesta(EtapaRepCliente e){
		long diff = 0;
		if (e.getFechaRealFin()!=null)
			diff = (e.getFechaRealFin().getTime()-e.getFechaInicio().getTime())/3600000;
		else 
			diff = ((new Date()).getTime()-e.getFechaInicio().getTime())/3600000;
		if (diff < 12)
			return "g";
		else if (diff < 24)
			return "y";
		else  if (diff < 48)
			return "o";
		else if (diff >48)
			return "r";
		else return "na";
	}
	
	public String calcTiempoRespuesta(ReparacionCliente rep){
		long diff = 0;
		for (EtapaRepCliente e : rep.getEtapasReparacion()){
			if (e.getFechaInicio()!=null && e.getFechaRealFin()==null){
				
				diff = ((new Date()).getTime()-e.getFechaInicio().getTime())/3600000;
				if (diff < 12)
					return "g";
				else if (diff < 24)
					return "y";
				else  if (diff < 48)
					return "o";
				else if (diff >48)
					return "r";
				else return "na";	
			}
		}
		return "na";
	
	}
	
	public String calcTiempoRespuesta(Integer idEtaRepCiente){
		List <EtapaRepCliente> etaRepClis = new ArrayList<EtapaRepCliente>();
		etaRepClis.add( (EtapaRepCliente) getEntityManager().createQuery("SELECT e FROM EtapaRepCliente e WHERE id=:id")
				.setParameter("id", idEtaRepCiente)
				.getSingleResult());
		EtapaRepCliente e = etaRepClis.get(0);
			if (e.getFechaInicio()!=null){
				long diff = 0;
				if (e.getFechaRealFin()!=null)
					diff = (e.getFechaRealFin().getTime()-e.getFechaInicio().getTime())/3600000;
				else 
					diff = ((new Date()).getTime()-e.getFechaInicio().getTime())/3600000;
				
					if (diff < 12)
						return "g";
					else if (diff < 24)
						return "y";
					else  if (diff < 48)
						return "o";
					else if (diff >48)
						return "r";
					else return "na";	
			}
		return "na";
	}
	
	
	public void getResultList() {
		etapasRepCli = getEntityManager()
				.createNativeQuery(
						"SELECT * FROM etapa_rep_cliente e "
								+ "	WHERE e.reparacionCli = :rep ORDER BY e.etapaRep.orden ASC")
				.setParameter("rep", this.reparacionClienteHome.getInstance())
				.getResultList();
	}

	public boolean aprobarEtapa() {
			
		setAccionEta("APR");
		
		
		return this.modify();
	}

	public boolean rechazarEtapa() {
		setAccionEta("REC");
		return this.modify();
	}
	
	public boolean noAprobarEtapa()
	{
		setAccionEta("NAP");
		return this.modify();
	}

	@Override
	public boolean preSave() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean preModify() {
		
		
		
		if(!accionEta.equals("NAP"))
		{
			//Validar si se ingreso al menos un servicio o descripcion adicional(componente defectuoso, defecto de capsula)
			if(instance.getEtapaRep().getId().equals((41)) && instance.getEstado().equals("PEN"))
			{
				
				if(reparacionClienteHome.getServiciosRep().isEmpty() && reparacionClienteHome.getSelDefectosCap().isEmpty() && reparacionClienteHome.getSelComponentesDef().isEmpty())
				{
					FacesMessages.instance().add(Severity.WARN,"Debe agregar al menos un servicio o descripcion adicional");
					return false;
				}
				
			}
			
			//Verificamos si la etapa elaboracion de molde o ensamblaje tiene requisiciones pendientes
			if(instance.getEtapaRep().getNombre().equals("Elaboracion de molde") || instance.getEtapaRep().getNombre().equals("Ensamblaje"))
			{
				for(RequisicionEtapaRep requi: instance.getRequisicionesEtapa())
				{
					if(requi.getEstado().equals("PEN"))
					{
						FacesMessages.instance().add(Severity.ERROR,sainv_messages.get("repCliHome_err_req_pen"));
						return false;
					}
				}
			}
		}
		
		
		// Verificamos si se trata de una aprobacion o de una eliminacion
		if (accionEta.equals("APR")) {
			// Verificamos que las cotizaciones se ingresen como requisiciones
			for (RequisicionEtapaRep tmpReq : instance.getRequisicionesEtapa()) {
				if (tmpReq.getEstado().equals("PEN"))
					FacesMessages.instance().add(
							Severity.ERROR,
							sainv_messages
									.get("etapaRepCliHome_error_reqnotapr"));

			}
			instance.setEstado("APR");
			instance.setFechaRealFin(new Date());
			instance.setUsuario(loginUser.getUser());
		}
		else if(accionEta.equals("NAP"))
		{
			System.out.println("Entro a NAP");
			// Verificamos que las cotizaciones se ingresen como requisiciones
			for (RequisicionEtapaRep tmpReq : instance.getRequisicionesEtapa()) {
				if (tmpReq.getEstado().equals("PEN"))
					FacesMessages.instance().add(
							Severity.ERROR,
							sainv_messages
									.get("etapaRepCliHome_error_reqnotapr"));

			}
			instance.setEstado("PEN");
			instance.setFechaRealFin(new Date());
			instance.setUsuario(loginUser.getUser());
			
		}
		else {
			descRechazo = instance.getDescripcion();
			instance.setEstado(null);
			instance.setFechaRealFin(null);
			instance.setUsuario(null);
		}
		return true;
	}

	@Override
	public boolean preDelete() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void posSave() {
		// TODO Auto-generated method stub

	}

	@Override
	public void posModify() {
		
		if (accionEta.equals("APR")) 
		{
			// Colocamos la siguiente etapa como pendiente
			List<EtapaRepCliente> siguienteEtapa = getEntityManager()
					.createQuery(
							"SELECT er FROM EtapaRepCliente er "
									+ "	WHERE er.reparacionCli = :rep AND (er.estado is null or er.estado <> 'APR' and er.estado <> 'NAP') "
									+ "	AND er <> :currEta ORDER BY er.etapaRep.orden ASC")
					.setParameter("rep", instance.getReparacionCli())
					.setParameter("currEta", instance).getResultList();
			if (siguienteEtapa != null && siguienteEtapa.size() > 0) 
			{
				EtapaRepCliente tmpEta = (EtapaRepCliente) siguienteEtapa.get(0);
				
					if(tmpEta.getEtapaRep().getOrden()==7)
					{
								tmpEta.setEstado("PEN");
						
								Calendar cal = new GregorianCalendar();
								cal.add(Calendar.DATE, 1);
								tmpEta.setFechaInicio(cal.getTime());
								getEntityManager().merge(tmpEta);
			
								for (RequisicionEtapaRep tmpReq : instance
										.getRequisicionesEtapa()) {
									// Si hay cotizaciones ingresadas, las convertimos a
									// requisiciones
									if (tmpReq.getEstado().equals("COT")) {
										tmpReq.setEstado("PEN");
										tmpReq.setEtapaRepCli(tmpEta);
										getEntityManager().merge(tmpReq);
									}
								}
								getEntityManager().flush();
								// SI se esta aprobando la evaluacion se setea el flag para que
								// el cliente decida si se aprueba o no
								if (instance.getEtapaRep().getCodEta().equals("EVA"))
									reparacionClienteHome.getInstance().setAprobada(false);
								else
									reparacionClienteHome.getInstance().setAprobada(true);
								
								
								reparacionClienteHome.getInstance().setEstado("FIN");
								reparacionClienteHome.getInstance().setFechaFin(new Date());
						
						
						
					}
					else
					{
						tmpEta.setEstado("PEN");
						
								Calendar cal = new GregorianCalendar();
								cal.add(Calendar.DATE, 1);
								tmpEta.setFechaInicio(cal.getTime());
								getEntityManager().merge(tmpEta);
			
								for (RequisicionEtapaRep tmpReq : instance
										.getRequisicionesEtapa()) {
									// Si hay cotizaciones ingresadas, las convertimos a
									// requisiciones
									if (tmpReq.getEstado().equals("COT")) {
										tmpReq.setEstado("PEN");
										tmpReq.setEtapaRepCli(tmpEta);
										getEntityManager().merge(tmpReq);
									}
								}
								getEntityManager().flush();
								// SI se esta aprobando la evaluacion se setea el flag para que
								// el cliente decida si se aprueba o no
								if (instance.getEtapaRep().getCodEta().equals("EVA"))
									reparacionClienteHome.getInstance().setAprobada(false);///Se necesita???
								else
									reparacionClienteHome.getInstance().setAprobada(true);
					}
				
				

			} 
			else //if(siguienteEtapa == null || siguienteEtapa.size() == 0 || siguienteEtapa.get(0).getEtapaRep().getOrden()==7) 
			{ 			// Si ya no hay ninguna otra etapa que este pendiente,
						// significa que el trabajo ha sido finalizado,
						// actualizamos el estado
				reparacionClienteHome.getInstance().setEstado("FIN");
				reparacionClienteHome.getInstance().setFechaFin(new Date());

			/*
				// Adicionamos una garantia de reparacion si es que no tiene o
				// tiene una no vigente
				if (!reparacionClienteHome.tieneGarantiaVigente(
						reparacionClienteHome.getInstance().getAparatoRep()
								.getFechaGarRep(), reparacionClienteHome
								.getInstance().getAparatoRep()
								.getPeriodoGarantiaRep())) {
					Calendar calMan = new GregorianCalendar();
					calMan.add(Calendar.DATE, 1);
					reparacionClienteHome.getInstance().getAparatoRep()
							.setFechaGarRep(calMan.getTime());
					reparacionClienteHome.getInstance().getAparatoRep()
							.setPeriodoGarantiaRep(90);
					getEntityManager()
							.merge(reparacionClienteHome.getInstance()
									.getAparatoRep());
				}

				*/
				
				
				
				
				
				
				/*
				// Guardamos el detalle de la venta para que aparezca en la
				// pantalla de cobros (incluir requi y costos)
				Double totalReparacion = 0d;
				VentaProdServ vta = new VentaProdServ();
				vta.setCliente(instance.getReparacionCli().getCliente());
				vta.setDetalle(instance.getReparacionCli().getDescripcion());
				vta.setEmpresa(loginUser.getUser().getSucursal().getEmpresa());
				vta.setEstado("PEN");
				vta.setFechaVenta(new Date());
				vta.setIdDetalle(instance.getReparacionCli().getId());
				vta.setMonto(0.0f);
				vta.setSucursal(loginUser.getUser().getSucursal()); // Cambiar
																	// para que
																	// al
																	// gaurdar
																	// reparacion
																	// guarde la
																	// sucursal
				vta.setTipoVenta("TLL");
				vta.setUsrEfectua(loginUser.getUser());
				getEntityManager().persist(vta);
				// Requisiciones
				List<ItemRequisicionEta> itemsRequis = getEntityManager()
						.createQuery(
								"SELECT i FROM ItemRequisicionEta i WHERE i.reqEtapa.etapaRepCli.reparacionCli = :rep")
						.setParameter("rep", instance.getReparacionCli())
						.getResultList();
				if (itemsRequis != null && itemsRequis.size() > 0)
					for (ItemRequisicionEta tmpItr : itemsRequis) {
						DetVentaProdServ dtVta = new DetVentaProdServ();
						dtVta.setCantidad(tmpItr.getCantidad());
						StringBuilder bld = new StringBuilder();
						bld.append(tmpItr.getProducto().getNombre());
						bld.append(", Modelo "
								+ tmpItr.getProducto().getModelo());
						bld.append(", Marca "
								+ tmpItr.getProducto().getMarca().getNombre());
						dtVta.setDetalle(bld.toString());
						dtVta.setMonto(tmpItr.getProducto().getPrcNormal());
						dtVta.setVenta(vta);
						dtVta.setCodClasifVta(tmpItr.getProducto()
								.getReferencia());
						dtVta.setCodClasifVta("PRQ");
						totalReparacion += dtVta.getMonto()
								* dtVta.getCantidad();
						getEntityManager().persist(dtVta);
					}

				for (ServicioReparacion srv : reparacionClienteHome
						.getInstance().getServiciosRep()) {
					if(srv.getEstado() == null || !srv.getEstado().equals("CBR")) {
						totalReparacion += srv.getServicio().getCosto();
						DetVentaProdServ dtVta = new DetVentaProdServ();
						dtVta.setCantidad(1);
						StringBuilder bld = new StringBuilder();
						bld.append(srv.getServicio().getName());
						dtVta.setDetalle(bld.toString());
						dtVta.setEscondido(true);
						dtVta.setMonto(srv.getServicio().getCosto().floatValue());
						dtVta.setVenta(vta);
						dtVta.setCodClasifVta(srv.getServicio().getCodigo());
						dtVta.setServicio(srv.getServicio());
						getEntityManager().persist(dtVta);
					}
				}

				// Actualizamos el monto de la venta
				getEntityManager().refresh(vta);
				vta.setMonto(super.moneyDecimal(totalReparacion).floatValue());
				instance.getReparacionCli().setCosto(vta.getMonto());
				getEntityManager().merge(vta);
				
				
				// Verificamos, si hay una garantia de venta o reparacion vigente,
				// se aprueba la venta con descuento del 100 por ciento
				if (reparacionClienteHome.tieneGarantiaVigente(
						reparacionClienteHome.getInstance().getAparatoRep().getFechaGarRep(), 
						reparacionClienteHome.getInstance().getAparatoRep().getPeriodoGarantiaRep()) ||
						reparacionClienteHome.tieneGarantiaVigente(
								reparacionClienteHome.getInstance().getAparatoRep().getFechaAdquisicion(), 
								reparacionClienteHome.getInstance().getAparatoRep().getPeriodoGarantia())) {
					ventaProdServHome.select(vta);
					ventaProdServHome.setLlevaDescuento(true);
					ventaProdServHome.getInstance().setCantidadDescuento(new Long(Math.round(vta.getMonto()*100))/100.0);
					ventaProdServHome.getInstance().setTipoDescuento("M");
					ventaProdServHome.getInstance().setDetalle("Cobro con descuento del 100% por cobertura de garantía");
					ventaProdServHome.getInstance().setEstado("PDS");
					ventaProdServHome.getInstance().setUsrDescuento(instance.getUsuario());
					ventaProdServHome.aprobarVta();
				}
				*/

			}
			// Actualizamos por si tiene precios nuevos o algo asi
			for (ServicioReparacion tmpSrvR : reparacionClienteHome
					.getInstance().getServiciosRep())
				getEntityManager().remove(tmpSrvR);

			getEntityManager().flush();
			for (ServicioReparacion tmpSrvR : reparacionClienteHome
					.getServiciosRep()) {
				tmpSrvR.setId(null);
				getEntityManager().persist(tmpSrvR);
			}

				FacesMessages.instance().clear();
			FacesMessages.instance().add(
					sainv_messages.get("etarepcli_etapa_fin"));
		} 
		
		//Codigo para no aprobar una orden de laboratorio que no tiene reparacion
		else if(accionEta.equals("NAP"))
		{
			System.out.println("Entro a NAP Pos Modify");
			
			// Consultamos todas las etapas pendientes
			List<EtapaRepCliente> siguienteEtapa = getEntityManager()
					.createQuery(
							"SELECT er FROM EtapaRepCliente er "
									+ "	WHERE er.reparacionCli = :rep AND (er.estado is null or er.estado <> 'APR') "
									+ "	AND er <> :currEta ORDER BY er.etapaRep.orden ASC")
					.setParameter("rep", instance.getReparacionCli())
					.setParameter("currEta", instance).getResultList();
			
			//Actualizamos el estado de la etapa actual a aprobado (seria diagnostico). Ya que el diagnostico si debe realizarse
			getInstance().setEstado("APR");
			getEntityManager().merge(instance);
			
			//actualizamos los demas estado a No aprobados(NAP) excepto 'Entrega'
			if (siguienteEtapa != null && siguienteEtapa.size() > 0) 
			{
				
				for(EtapaRepCliente etpas: siguienteEtapa)
				{
					if(!etpas.getEtapaRep().getNombre().equals("Entrega"))
					{
						
						etpas.setEstado("NAP");
						getEntityManager().merge(etpas);
						
					}
					else
					{
						etpas.setEstado("APR");
						getEntityManager().merge(etpas);
						break;
					}
					
					
				}
					
				
				/*
				EtapaRepCliente tmpEta = (EtapaRepCliente) siguienteEtapa
						.get(0);
				tmpEta.setEstado("PEN");
				Calendar cal = new GregorianCalendar();
				cal.add(Calendar.DATE, 1);
				tmpEta.setFechaInicio(cal.getTime());
				getEntityManager().merge(tmpEta);*/
				
				

				/*for (RequisicionEtapaRep tmpReq : instance
						.getRequisicionesEtapa()) {
					// Si hay cotizaciones ingresadas, las convertimos a
					// requisiciones
					if (tmpReq.getEstado().equals("COT")) {
						tmpReq.setEstado("PEN");
						tmpReq.setEtapaRepCli(tmpEta);
						getEntityManager().merge(tmpReq);
					}
				}*/
			
			
				getEntityManager().flush();
				// SI se esta aprobando la evaluacion se setea el flag para que
				// el cliente decida si se aprueba o no
				
				
				/*if (instance.getEtapaRep().getCodEta().equals("EVA"))
					reparacionClienteHome.getInstance().setAprobada(false);
				else
					reparacionClienteHome.getInstance().setAprobada(true);*/
				reparacionClienteHome.getInstance().setEstado("FIN");
				reparacionClienteHome.getInstance().setFechaFin(new Date());
			} 
			
			else 
			{ // Si ya no hay ninguna otra etapa que este pendiente,
						// significa que el trabajo ha sido finalizado,
						// actualizamos el estado
				reparacionClienteHome.getInstance().setEstado("FIN");
				reparacionClienteHome.getInstance().setFechaFin(new Date());
			}
			
			// Actualizamos por si tiene precios nuevos o algo asi
			for (ServicioReparacion tmpSrvR : reparacionClienteHome
					.getInstance().getServiciosRep())
				getEntityManager().remove(tmpSrvR);

			getEntityManager().flush();
			for (ServicioReparacion tmpSrvR : reparacionClienteHome
					.getServiciosRep()) {
				tmpSrvR.setId(null);
				getEntityManager().persist(tmpSrvR);
			}

				FacesMessages.instance().clear();
			FacesMessages.instance().add(
					sainv_messages.get("etarepcli_etapa_fin"));
			
		}
		
		
		else {
			String tmpDesc = "";
			// Limpiamos la fecha de finalizacion de la etapa y los comentarios,
			// y los borramos
			List<EtapaRepCliente> etapasPasadas = getEntityManager()
					.createQuery(
							"SELECT er FROM EtapaRepCliente er WHERE er.reparacionCli = :rep "
									+ "	ORDER BY er.etapaRep.orden DESC")
					.setParameter("rep", instance.getReparacionCli())
					.getResultList();
			for (EtapaRepCliente tmpEt : etapasPasadas) {
				tmpDesc = tmpEt.getDescripcion();
				tmpEt.setFechaRealFin(null);
				tmpEt.setDescripcion(null);
				tmpEt.setUsuario(null);
				tmpEt.setEstado(null);
				if (tmpEt.getEtapaRep().equals(
						instance.getEtapaRep().getLoopBack())) {
					tmpDesc = descRechazo + " - " + tmpDesc;
					tmpEt.setDescripcion(tmpDesc);
					tmpEt.setEstado("PEN");
					getEntityManager().merge(tmpEt);
					getEntityManager().flush();
					break;
				}
				getEntityManager().merge(tmpEt);
			}
			getEntityManager().flush();
			FacesMessages.instance().clear();
			FacesMessages.instance().add(
					sainv_messages.get("etarepcli_etapa_rec"));
		}
		// Ya sea que aprobamos o no, guardamos las condiciones del aparato que fueron cambiadas
		reparacionClienteHome.modify();
		// Reseteamos la lista de las reparaciones pendientes para el usuario
		repsPendientes();
	}

	@Override
	public void posDelete() {
		// TODO Auto-generated method stub

	}

	public List<Object[]> getEtapasRepCli() {
		return etapasRepCli;
	}

	public void setEtapasRepCli(List<Object[]> etapasRepCli) {
		this.etapasRepCli = etapasRepCli;
	}

	public Integer getEtaRepId() {
		return etaRepId;
	}

	public void setEtaRepId(Integer etaRepId) {
		this.etaRepId = etaRepId;
	}

	public String getAccionEta() {
		return accionEta;
	}

	public void setAccionEta(String accionEta) {
		this.accionEta = accionEta;
	}

	public List<ItemRequisicionEta> getItmsRequi() {
		return itmsRequi;
	}

	public void setItmsRequi(List<ItemRequisicionEta> itmsRequi) {
		this.itmsRequi = itmsRequi;
	}

	public String getNomCoinci() {
		return nomCoinci;
	}

	public void setNomCoinci(String nomCoinci) {
		this.nomCoinci = nomCoinci;
	}

}
