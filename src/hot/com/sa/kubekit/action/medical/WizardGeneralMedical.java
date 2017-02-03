package com.sa.kubekit.action.medical;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.faces.FacesMessages;

import com.sa.kubekit.action.i18n.KubeBundle;
import com.sa.kubekit.action.sales.VentaProdServHome;
import com.sa.model.medical.ClinicalHistory;
import com.sa.model.medical.DiagnosticoConsulta;
import com.sa.model.medical.ExamenConsulta;
import com.sa.model.medical.GeneralInformation;
import com.sa.model.medical.GeneralMedical;
import com.sa.model.medical.MedicalAppointmentService;
import com.sa.model.medical.RecomendacionConsulta;
import com.sa.model.medical.id.MedicalAppointmentServiceId;
import com.sa.model.sales.DetVentaProdServ;
import com.sa.model.sales.Service;
import com.sa.model.sales.VentaProdServ;

@Name("wizardGeneralMedical")
@Scope(ScopeType.CONVERSATION)
public class WizardGeneralMedical extends WizardClinicalHistory {
	@In(create = true)
	protected MedicalAppointmentDAO medicalAppointmentDAO;

	@In(create = true)
	private GeneralMedicalDAO generalMedicalDAO;
	
	@In(create = true)
	protected ClienteHome clienteHome;
	
	@In(create = true)
	protected AntecedenteHome antecedenteHome;
	
	@In(required=false, create=true)
	private PrescriptionHome prescriptionHome;
	
	@In(required = true)
	protected KubeBundle sainv_messages;
	
	private String paginaAnterior;

	public WizardGeneralMedical() {
		// cofiguramos las vistas de navegacion generales
		linkDiagBack = "/medical/clinicalHistory/generalMedical/step2.xhtml";
		linkDiagNext = "/medical/clinicalHistory/generalMedical/stepFinal.xhtml";
		linkEndBack = "/medical/clinicalHistory/generalMedical/stepDiagnostic.xhtml";
		linkEndNext = "/medical/clinicalHistory/generalMedical/stepFinal.xhtml";
	}

	/*public void load() {
		
		if (getConsecutive() != null) {
			if (!generalMedicalDAO.isManaged()) {
				
				GeneralMedical instance = entityManager.find(
						GeneralMedical.class, getConsecutive());
				if (instance != null) {
					generalMedicalDAO.select(instance);
					super.load();
				}
			}
			System.out.println("Paso3");
		} else {
			this.init();
		}
		
		prescriptionHome.setInstance(generalMedicalDAO.getInstance().getMedicalAppointment().getPrescription());
	}*/
	public void load() {
		//Conversation.instance().end();
		//Conversation.instance().begin();
		//this.generalMedicalDAO.clean();
		/*super.generalContainer.setMode("w");
		setMode("w");
			setConsecutive(null);
			super.setMode("w");
			quitado 21/12/2016 cuando se puso? */
		
		
			System.out.println("*** Cargo el evento load principal: ");
			
			
			if (getConsecutive() != null) {
				if (!generalMedicalDAO.isManaged()) {
					
					GeneralMedical instance = entityManager.find(
							GeneralMedical.class, getConsecutive());
					if (instance != null) {
						generalMedicalDAO.select(instance);
						super.load();
					}
				}
				System.out.println("Paso3");
			} else {
				
				this.init();
				antecedenteHome.load();//Para carcar la lista de antecedentes disponibles
			}
			
			antecedenteHome.cargarAntecedentesPaciente(clienteHome.getInstance());//Para cargar antecedente de pacientes
			
			prescriptionHome.setInstance(generalMedicalDAO.getInstance().getMedicalAppointment().getPrescription());
			
		System.out.println("Id ConversationLoad "+ Conversation.instance().getId());
		
		}
	
	
	//Para boton atender cita
	public void cargarNuevaCita()
	{
		this.init();
		prescriptionHome.setInstance(generalMedicalDAO.getInstance().getMedicalAppointment().getPrescription());
	}
	
	
	//@Begin//Para boton verHistorial
	public void cargarHistorial()
	{
		if (!generalMedicalDAO.isManaged()) {
			
			GeneralMedical instance = entityManager.find(
					GeneralMedical.class, getConsecutive());
			if (instance != null) {
				generalMedicalDAO.select(instance);
				super.load();
			}
		}
		System.out.println("Paso3");
	}
	
	
	
	public void load2() {
		//Conversation.instance().end();
		//Conversation.instance().begin();
		
		if (getConsecutive() != null) {
			System.out.println("Paso1");
			GeneralMedical instance = entityManager.find(
					GeneralMedical.class, getConsecutive());
			if (instance != null) {
				generalMedicalDAO.select(instance);
				try {
					super.load2();
				} catch (Exception e) {
					System.err.println("error en super.load(): " + e);
				}
			}
		} else {
			System.out.println("entro a crear el .init()");
			this.init();
		}
		
		antecedenteHome.cargarAntecedentesPaciente(clienteHome.getInstance());//Para cargar antecedente de pacientes
		prescriptionHome.setInstance(generalMedicalDAO.getInstance().getMedicalAppointment().getPrescription());
		
		System.out.println("Id ConversationLoad2 "+ Conversation.instance().getId());
	}
	
	public void cerrarConver(Conversation conver)
	{
		conver.end();
	}
	
	public void mostrarIdC(int idC)
	{
		System.out.println("Id conversation "+idC);
	}

	public void init() {
		super.init();
		// configuraciones iniciales
		if (clienteHome.getInstance().getGeneralInformation() == null) {
			GeneralInformation gi = new GeneralInformation(clienteHome.getInstance());
			entityManager.persist(gi);
			clienteHome.getInstance().setGeneralInformation(gi);
			
		}
		generalMedicalDAO.getInstance().setCliente(
				medicalAppointmentDAO.getInstance().getCliente());
		generalMedicalDAO.getInstance().setMedicalAppointment(
				medicalAppointmentDAO.getInstance());
		
		//Nuevo
		//generalMedicalDAO.getInstance().getMedicalAppointment().set
	}

	// metodos step1
	public String step1() {
		try {
			if (clienteHome.getInstance().getGeneralInformation().getId() == null) {
				entityManager.persist(clienteHome.getInstance()
						.getGeneralInformation());
			} else {
				entityManager.merge(clienteHome.getInstance()
						.getGeneralInformation());
			}
			if (clienteHome.modify()) {
				return "next";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// metodos step2
	public String step2() {
		// validamos datos del paso dos
		return step1();
	}

	// metodos step3
	public String step3() {
		// validamos datos del paso tres
		return "next"; 
	}
	
	public String allSteps(){
		if(prescriptionHome.getExamenesAgregados().isEmpty() && prescriptionHome.getServiciosAgregados().isEmpty()){
			FacesMessages.instance().add("No hay ningun examen o servicio agregado");
			
			return "";
		}
		else
		{
		
				try {
					if (clienteHome.getInstance().getGeneralInformation().getId() == null) {
						entityManager.persist(clienteHome.getInstance()
								.getGeneralInformation());
					} else {
						entityManager.merge(clienteHome.getInstance()
								.getGeneralInformation());
					}
					if (clienteHome.modify()) {
						//return "next";
					}
				} catch (Exception e) {
					System.out.println("catch allsteps: " + e);
					e.printStackTrace();
				}
				createDiagnostics();
				//Verificamos si se diagnostico sordera
						if(prescriptionHome.isDiagnSordera())
							clienteHome.getInstance().setDiagnosSordera(new Date());
						
						super.stepFinal();
						
						generalMedicalDAO.setMedicamentos(prescriptionHome.getItemsAgregados());
						
						if (!generalMedicalDAO.isManaged()) {
							if (generalMedicalDAO.save()){
								
								//Guardamos los diagnosticos, recomendaciones, examenes, servicios adicionales
								for(RecomendacionConsulta recCon : prescriptionHome.getRecomendacionesAgregadas()) {
									recCon.setConsulta(generalMedicalDAO.getInstance());
									recCon.setNomRecomendacion(recCon.getRecomendacion().getNombre());
									
									System.out.println("** consulta "+generalMedicalDAO.getInstance().getMedicalAppointment().getId());
									System.out.println("** historial "+generalMedicalDAO.getInstance().getObservation());
									
									entityManager.persist(recCon);
								}
								
								for(DiagnosticoConsulta digCon : prescriptionHome.getDiagnosticosAgregados()) {
									digCon.setConsulta(generalMedicalDAO.getInstance());
									digCon.setNomDiagnostico(digCon.getDiagnostico().getNombre());
									entityManager.persist(digCon);
								}
								
								//Examenes
								for(ExamenConsulta exaCon : prescriptionHome.getExamenesAgregados()) {
									exaCon.setConsulta(generalMedicalDAO.getInstance());
									exaCon.setNomExamen(exaCon.getExamen().getName());
									entityManager.persist(exaCon);
								}
								
								for(MedicalAppointmentService srv : prescriptionHome.getServiciosAgregados()) {
									if(medicalAppointmentDAO.getAppointmentItems().contains(srv)) 
										medicalAppointmentDAO.getAppointmentItems().remove(srv);
									else {
										//srv.setServiceClinicalHistory();
										MedicalAppointmentServiceId id = new MedicalAppointmentServiceId(
												medicalAppointmentDAO.getInstance().getId(), srv.getService().getId());
										MedicalAppointmentService med = new MedicalAppointmentService();
										med.setMedicalAppointmentServiceId(id);
										med.setService(srv.getService());
										med.setMedicalAppointment(medicalAppointmentDAO.getInstance());
										entityManager.persist(med);
									}
								}
								
								saveDiagnostics();
								prescriptionHome.getInstance().setMedicalAppointment(generalMedicalDAO.getInstance().getMedicalAppointment());
								prescriptionHome.save();
								FacesMessages.instance().add(sainv_messages
										.get("history_created"));
								//Generamos un detalle de la venta
								Double totalReparacion = 0d;
								VentaProdServ vta = new VentaProdServ();
								vta.setCliente(clienteHome.getInstance());
								vta.setDetalle("Servicios medicos - " + medicalAppointmentDAO.getInstance().getComment());
								vta.setEmpresa(medicalAppointmentDAO.getLoginUser().getUser().getSucursal().getEmpresa());
								
								vta.setEstado("PEN");
								vta.setFechaVenta(new Date());
								vta.setIdDetalle(medicalAppointmentDAO.getInstance().getId());
								vta.setMonto(0.0f);
								//vta.setSucursal(medicalAppointmentDAO.getLoginUser().getUser().getSucursal()); //Cambiar para que al gaurdar reparacion guarde la sucursal
								vta.setSucursal(medicalAppointmentDAO.getInstance().getSucursal());//Se cambio a que la venta sera tomada en cuenta segun la sucursal especificada en la cita, esta sera espeficiada por la persona que la prog
								System.out.println("*******************1111 sucursal "+ medicalAppointmentDAO.getInstance().getSucursal());
								vta.setTipoVenta("CST");
								vta.setUsrEfectua(medicalAppointmentDAO.getLoginUser().getUser());
								entityManager.persist(vta);
								//Servicios registrados 
								entityManager.flush();
								entityManager.refresh(medicalAppointmentDAO.getInstance());
								entityManager.refresh(generalMedicalDAO.getInstance());
								List<Service> serviciosCobrados = new ArrayList<Service>();
								for(MedicalAppointmentService tmpSrv: medicalAppointmentDAO.getInstance().getMedicalAppointmentServices()) {
									serviciosCobrados.add(tmpSrv.getService());
									DetVentaProdServ dtVta = new DetVentaProdServ();
									dtVta.setCantidad(1);
									StringBuilder bld = new StringBuilder();
									bld.append(tmpSrv.getService().getName());
									dtVta.setCodClasifVta("SRV");
									dtVta.setCodExacto(tmpSrv.getService().getCodigo());
									dtVta.setServicio(tmpSrv.getService());
									dtVta.setEscondido(false);
									dtVta.setDetalle(bld.toString());
									dtVta.setMonto(tmpSrv.getService().getCosto().floatValue());
									dtVta.setVenta(vta);
									totalReparacion += dtVta.getMonto()*dtVta.getCantidad();
									entityManager.persist(dtVta);
								}
								//Y los examenes tambien se adjuntan al cobro
								for(ExamenConsulta tmpSrv: generalMedicalDAO.getInstance().getExamenes()) {
									if(!serviciosCobrados.contains(tmpSrv.getExamen())) {
										DetVentaProdServ dtVta = new DetVentaProdServ();
										dtVta.setCantidad(1);
										StringBuilder bld = new StringBuilder();
										bld.append(tmpSrv.getExamen().getName());
										dtVta.setCodClasifVta("SRV");
										dtVta.setCodExacto(tmpSrv.getExamen().getCodigo());
										dtVta.setServicio(tmpSrv.getExamen());
										dtVta.setEscondido(false);
										dtVta.setDetalle(bld.toString());
										dtVta.setMonto(tmpSrv.getExamen().getCosto().floatValue());
										dtVta.setVenta(vta);
										totalReparacion += dtVta.getMonto()*dtVta.getCantidad();
										entityManager.persist(dtVta);
									}
								}
								//Actualizamos el monto de la venta
								entityManager.refresh(vta);
								vta.setMonto(new VentaProdServHome().moneyDecimal(totalReparacion).floatValue());
								entityManager.merge(vta);
								
								antecedenteHome.persistirAntecedentesLista();
							}
						} else if (generalMedicalDAO.modify()){
							saveDiagnostics();
							prescriptionHome.modify();
							FacesMessages.instance().add(sainv_messages
									.get("history_modified"));
						}
						
		
				return "exito";
		}
	}
	
	@Override
	public String stepFinal() {
		//Verificamos si se diagnostico sordera
		if(prescriptionHome.isDiagnSordera())
			clienteHome.getInstance().setDiagnosSordera(new Date());
		
		super.stepFinal();
		
		generalMedicalDAO.setMedicamentos(prescriptionHome.getItemsAgregados());
		
		if (!generalMedicalDAO.isManaged()) {
			if (generalMedicalDAO.save()){
				
				//Guardamos los diagnosticos, recomendaciones, examenes, servicios adicionales
				for(RecomendacionConsulta recCon : prescriptionHome.getRecomendacionesAgregadas()) {
					recCon.setConsulta(generalMedicalDAO.getInstance());
					System.out.println("** consulta "+generalMedicalDAO.getInstance().getMedicalAppointment().getId());
					System.out.println("** historial "+generalMedicalDAO.getInstance().getObservation());
					
					recCon.setNomRecomendacion(recCon.getRecomendacion().getNombre());
					entityManager.persist(recCon);
				}
				
				for(DiagnosticoConsulta digCon : prescriptionHome.getDiagnosticosAgregados()) {
					digCon.setConsulta(generalMedicalDAO.getInstance());
					digCon.setNomDiagnostico(digCon.getDiagnostico().getNombre());
					entityManager.persist(digCon);
				}
				
				for(ExamenConsulta exaCon : prescriptionHome.getExamenesAgregados()) {
					exaCon.setConsulta(generalMedicalDAO.getInstance());
					exaCon.setNomExamen(exaCon.getExamen().getName());
					entityManager.persist(exaCon);
				}
				
				for(MedicalAppointmentService srv : prescriptionHome.getServiciosAgregados()) {
					if(medicalAppointmentDAO.getAppointmentItems().contains(srv)) 
						medicalAppointmentDAO.getAppointmentItems().remove(srv);
					else {
						//srv.setServiceClinicalHistory();
						MedicalAppointmentServiceId id = new MedicalAppointmentServiceId(
								medicalAppointmentDAO.getInstance().getId(), srv.getService().getId());
						MedicalAppointmentService med = new MedicalAppointmentService();
						med.setMedicalAppointmentServiceId(id);
						med.setService(srv.getService());
						med.setMedicalAppointment(medicalAppointmentDAO.getInstance());
						entityManager.persist(med);
					}
				}
				
				saveDiagnostics();
				prescriptionHome.getInstance().setMedicalAppointment(generalMedicalDAO.getInstance().getMedicalAppointment());
				prescriptionHome.save();
				FacesMessages.instance().add(sainv_messages
						.get("history_created"));
				//Generamos un detalle de la venta
				Double totalReparacion = 0d;
				VentaProdServ vta = new VentaProdServ();
				vta.setCliente(clienteHome.getInstance());
				vta.setDetalle("Servicios medicos - " + medicalAppointmentDAO.getInstance().getComment());
				vta.setEmpresa(medicalAppointmentDAO.getLoginUser().getUser().getSucursal().getEmpresa());
			
				
				vta.setEstado("PEN");
				vta.setFechaVenta(new Date());
				vta.setIdDetalle(medicalAppointmentDAO.getInstance().getId());
				vta.setMonto(0.0f);
				//vta.setSucursal(medicalAppointmentDAO.getLoginUser().getUser().getSucursal()); //Cambiar para que al gaurdar reparacion guarde la sucursal
				vta.setSucursal(medicalAppointmentDAO.getInstance().getSucursal());//Se cambio a que la venta sera tomada en cuenta segun la sucursal especificada en la cita, esta sera espeficiada por la persona que la prog
				System.out.println("*******************2222 sucursal "+ medicalAppointmentDAO.getInstance().getSucursal());
				vta.setTipoVenta("CST");
				vta.setUsrEfectua(medicalAppointmentDAO.getLoginUser().getUser());
				entityManager.persist(vta);
				//Servicios registrados 
				entityManager.flush();
				entityManager.refresh(medicalAppointmentDAO.getInstance());
				entityManager.refresh(generalMedicalDAO.getInstance());
				List<Service> serviciosCobrados = new ArrayList<Service>();
				for(MedicalAppointmentService tmpSrv: medicalAppointmentDAO.getInstance().getMedicalAppointmentServices()) {
					serviciosCobrados.add(tmpSrv.getService());
					DetVentaProdServ dtVta = new DetVentaProdServ();
					dtVta.setCantidad(1);
					StringBuilder bld = new StringBuilder();
					bld.append(tmpSrv.getService().getName());
					dtVta.setCodClasifVta("SRV");
					dtVta.setCodExacto(tmpSrv.getService().getCodigo());
					dtVta.setServicio(tmpSrv.getService());
					dtVta.setEscondido(false);
					dtVta.setDetalle(bld.toString());
					dtVta.setMonto(tmpSrv.getService().getCosto().floatValue());
					dtVta.setVenta(vta);
					totalReparacion += dtVta.getMonto()*dtVta.getCantidad();
					entityManager.persist(dtVta);
				}
				//Y los examenes tambien se adjuntan al cobro
				for(ExamenConsulta tmpSrv: generalMedicalDAO.getInstance().getExamenes()) {
					if(!serviciosCobrados.contains(tmpSrv.getExamen())) {
						DetVentaProdServ dtVta = new DetVentaProdServ();
						dtVta.setCantidad(1);
						StringBuilder bld = new StringBuilder();
						bld.append(tmpSrv.getExamen().getName());
						dtVta.setCodClasifVta("SRV");
						dtVta.setCodExacto(tmpSrv.getExamen().getCodigo());
						dtVta.setServicio(tmpSrv.getExamen());
						dtVta.setEscondido(false);
						dtVta.setDetalle(bld.toString());
						dtVta.setMonto(tmpSrv.getExamen().getCosto().floatValue());
						dtVta.setVenta(vta);
						totalReparacion += dtVta.getMonto()*dtVta.getCantidad();
						entityManager.persist(dtVta);
					}
				}
				//Actualizamos el monto de la venta
				entityManager.refresh(vta);
				vta.setMonto(new VentaProdServHome().moneyDecimal(totalReparacion).floatValue());
				entityManager.merge(vta);
			}
		} else if (generalMedicalDAO.modify()){
			saveDiagnostics();
			prescriptionHome.modify();
			FacesMessages.instance().add(sainv_messages
					.get("history_modified"));
		}
		return "exito";
	}
	
	

	@Override
	public ClinicalHistory obtainClinicalHistory() {
		return generalMedicalDAO.getInstance();
	}

	public String getPaginaAnterior() {
		return paginaAnterior;
	}

	public void setPaginaAnterior(String paginaAnterior) {
		this.paginaAnterior = paginaAnterior;
	}

	
	
	
	
}