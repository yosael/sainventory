package com.sa.kubekit.action.sales;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import com.sa.kubekit.action.inventory.MovimientoHome;
import com.sa.kubekit.action.inventory.ProductoHome;
import com.sa.kubekit.action.security.LoginUser;
import com.sa.kubekit.action.util.KubeDAO;
import com.sa.kubekit.action.workshop.AparatoClienteHome;
import com.sa.model.acct.CondicionPago;
import com.sa.model.acct.CuentaCobrar;
import com.sa.model.crm.Cliente;
import com.sa.model.inventory.CodProducto;
import com.sa.model.inventory.Inventario;
import com.sa.model.inventory.Item;
import com.sa.model.inventory.Movimiento;
import com.sa.model.inventory.Producto;
import com.sa.model.inventory.id.ItemId;
import com.sa.model.sales.ComboAparato;
import com.sa.model.sales.CostoServicio;
import com.sa.model.sales.CotCmbsItems;
import com.sa.model.sales.CotizacionComboApa;
import com.sa.model.sales.CotizacionComboItem;
import com.sa.model.sales.CotizacionCombos;
import com.sa.model.sales.CotizacionPrdSvcAdicionales;
import com.sa.model.sales.DetVentaProdServ;
import com.sa.model.sales.ItemComboApa;
import com.sa.model.sales.Service;
import com.sa.model.sales.VentaProdServ;
import com.sa.model.security.Sucursal;
import com.sa.model.workshop.AparatoCliente;
import com.sa.model.workshop.EtapaRepCliente;
import com.sa.model.workshop.EtapaReparacion;
import com.sa.model.workshop.ProcesoTaller;
import com.sa.model.workshop.ReparacionCliente;
import com.sa.kubekit.action.acct.ConceptoMovHome;
import com.sa.kubekit.action.acct.CuentaCobrarHome;

@Name("ventaComboHome")
@Scope(ScopeType.CONVERSATION)
public class VentaComboHome extends KubeDAO<VentaProdServ> {

	private static final long serialVersionUID = 1L;

	@In
	private LoginUser loginUser;

	@In(required = false, create = true)
	@Out
	private MovimientoHome movimientoHome;

	@In(required = false, create = true)
	@Out
	private VentaItemHome ventaItemHome;

	@In(required = false, create = true)
	@Out
	private AparatoClienteHome aparatoClienteHome;

	@In(required = false, create = true)
	private ProductoHome productoHome;

	@In(required = false, create = true)
	@Out
	private ConceptoMovHome conceptoMovHome;

	@In(required = false, create = true)
	@Out
	private CuentaCobrarHome cuentaCobrarHome;
	private List<Sucursal> sucursales = new ArrayList<Sucursal>();
	private List<VentaProdServ> resultList = new ArrayList<VentaProdServ>();
	private List<CostoServicio> costos = new ArrayList<CostoServicio>();
	private List<ItemComboApa> items = new ArrayList<ItemComboApa>();
	private Float totalCostos;
	private Float totalItems;
	private Float totalCostosBin;
	private Float totalItemsBin;
	private List<CodProducto> codsProducto;
	private ItemComboApa selItmCombo = null;
	private List<CodProducto> currCodigos = new ArrayList<CodProducto>();
	private List<ItemComboApa> itemsComboApa = new ArrayList<ItemComboApa>();
	private List<ItemComboApa> itemsComboApaBin = new ArrayList<ItemComboApa>();
	private List<CotizacionComboApa> cotizacionList = new ArrayList<CotizacionComboApa>();
	private List<ComboAparato> selCmbsList= new ArrayList<ComboAparato>();
	private List<ComboAparato> selCmbsListBin = new ArrayList<ComboAparato>();
	private CotizacionCombos cotizacionCombos = new CotizacionCombos();
	private Sucursal sucursalFlt;
	private Integer selectedComboVta;
	private Integer selectedComboVtaBin;
	private ComboAparato comboVta;
	private ComboAparato comboVtaBin;
	private Integer cotizId;
	private CotizacionComboApa cotizacion;
	private boolean binaural;
	private Short selBinaural;
	private boolean tieneGarantia;
	private boolean tieneGarantiaBin;
	private String nomCoinci = "";
	private Integer vtcId;
	private Float subTotal;
	private Float totalMagnanime;
	private Float totalservprod = 0f;
	private boolean showTotal = false;
	private int validez;
	private List<CotCmbsItems> itemsCotizadosBin = new ArrayList<CotCmbsItems>();
	private List<CotCmbsItems> itemsCotizados = new ArrayList<CotCmbsItems>();
	private Cliente nombreCompleto;
	private List<CotizacionPrdSvcAdicionales> prdSvcAdicionales = new ArrayList<CotizacionPrdSvcAdicionales>();
	private ComboAparato comboSelec;
	private CodProducto codCombo1;
	private CodProducto codCombo2;
	

	@Override
	public void create() {
		setRangoUlt30dias();
		setCreatedMessage(createValueExpression(sainv_messages
				.get("vtacmb_created")));
		setUpdatedMessage(createValueExpression(sainv_messages
				.get("vtacmb_updated")));
		setDeletedMessage(createValueExpression(sainv_messages
				.get("vtacmb_deleted")));
	}
	
	public int generarEntero()
	{
		Random rd = new Random();
		
		return rd.nextInt();
	}

	public void load() {
		try {
			setInstance(new VentaProdServ());
			ventaItemHome.clearInstance();
			ventaItemHome.setItemsAgregados(new ArrayList<Item>());
			setCotizacion(null);
			setSelBinaural((short) 1);
			if (getCotizId() != null && getCotizId() > 0) {
				// Cargamos los datos de la cotizacion
				CotizacionComboApa tmpCotiz = (CotizacionComboApa) getEntityManager()
						.createQuery(
								"SELECT c FROM CotizacionComboApa c WHERE c.id = :idCotiz AND c.cotizacionComboBin = NULL")
						.setParameter("idCotiz", cotizId).getSingleResult();
				instance.setCliente(tmpCotiz.getCliente());
				System.out.println("Nombre del cliente "
						+ tmpCotiz.getCliente().getNombres()
						+ tmpCotiz.getCliente().getApellidos()
						+ " nombre completo: "
						+ tmpCotiz.getCliente().getNombreCompleto());
				setCotizacion(tmpCotiz);
				instance.setDetalle(tmpCotiz.getDetalleAparato());
				System.out.println("tmpCotiz: "+ tmpCotiz.getDetalleAparato());
				System.out.println("instance: "+ instance.getDetalle() );
				aparatoClienteHome.getInstance().setRetroAuricular(
						tmpCotiz.isRetroAuricular());
				aparatoClienteHome.getInstance().setLadoAparato(
						tmpCotiz.getLadoAparato());
				setValidez(cotizacion.getValidez());
				// Cargamos la lista de combos cotizados (CotizacionCombos)
				List<CotizacionCombos> tmpCtCmbs = getEntityManager()
						.createQuery(
								"SELECT cc FROM CotizacionCombos cc WHERE cc.cotizacion.id = :cot")
						.setParameter("cot", getCotizId()).getResultList();
				// seteamos el valor del combo seleccionado por el m�dico
				System.out.println("EL COMBO seleccionado ES: "
						+ cotizacion.getSelComboId());
				if (cotizacion.getSelComboId() != null) {
					setSelectedComboVta(cotizacion.getSelComboId());
				}
				
				// Cargamos los productos y servicios adicionales cotizados.
				try {
					if (cotizacion.getCotizItmSvcAdi().size() > 0){
						List<Item> itemsCotizados = new ArrayList<Item>();
						List<Service> serviciosCotizados = new ArrayList<Service>();
						for (CotizacionPrdSvcAdicionales tmpCot : cotizacion.getCotizItmSvcAdi()){
							if (tmpCot.getProducto() != null){
								Item item = new Item();
								item.setCotItmId(tmpCot.getId());
								item.setCantidad(tmpCot.getCantidad());
								item.setCostoUnitario(tmpCot.getPrecioCotizado());
								item.setTipoPrecio(tmpCot.getTipPreCotizado());
								item.setInventario((Inventario)getEntityManager().createQuery("SELECT i FROM Inventario i WHERE i.producto = :prod AND i.sucursal = :suc")
										.setParameter("prod", tmpCot.getProducto())
										.setParameter("suc", cotizacion.getSucursal())
										.getSingleResult());
								item.setItemId(new ItemId());
								item.getItemId().setInventarioId(tmpCot.getProducto().getId());
								itemsCotizados.add(item);
								ventaItemHome.actualizarSubtotal();
							}
							if (tmpCot.getServicio()!=null){
								if(!serviciosCotizados.contains(tmpCot.getServicio())) 
									tmpCot.getServicio().setCantidad(tmpCot.getCantidad());
									serviciosCotizados.add(0, tmpCot.getServicio());
									actualizarSubtotalSrv();
							}
							ventaItemHome.setServiciosAgregados(serviciosCotizados);
							ventaItemHome.setItemsAgregados(itemsCotizados);
						}
					}
				} catch (Exception e){
					e.printStackTrace();
				}
				try {
					ComboAparato combo;
					List<ItemComboApa> preciosCotizados = new ArrayList<ItemComboApa>();
					selCmbsList= new ArrayList<ComboAparato>(); //lina nueva edwin
					for (CotizacionCombos tmpCmb : tmpCtCmbs) 
					{
						itemsCotizados = tmpCmb.getItemsCotizados();
						System.out.println("TAMA�O DE LISTA DE ITEMS: "
								+ itemsCotizados.size());
						combo = new ComboAparato();
						combo.setId(tmpCmb.getCombo().getId());
						combo.setCodigo(tmpCmb.getCombo().getCodigo());
						combo.setCostosCombo(tmpCmb.getCombo().getCostosCombo());
						preciosCotizados = new ArrayList<ItemComboApa>();int conteoNormal=0;
						// Vamos a iterar la lista de items que tiene el combo,
						for (ItemComboApa tmpItm : tmpCmb.getCombo().getItemsCombo()) 
						{
							ItemComboApa tmpNItem= new ItemComboApa();
							//tmpNItem=tmpItm;
							tmpNItem.setCantidad(tmpItm.getCantidad());
							tmpNItem.setCategoria(tmpItm.getCategoria());
							tmpNItem.setCodProducto(tmpItm.getCodProducto());
							tmpNItem.setCombo(tmpItm.getCombo());
							tmpNItem.setDescripcion(tmpItm.getDescripcion());
							tmpNItem.setId(tmpItm.getId());
							tmpNItem.setProducto(tmpItm.getProducto());
							tmpNItem.setPrincipal(tmpItm.isPrincipal());
							//tmpNItem.setPrecioCotizado(tmpItm.getPrecioCotizado());
							//tmpNItem.setTipoPrecio(tmpItm.getTipoPrecio());
							//tmpNItem.setTipPreCotizado(tmpItm.getTipPreCotizado());
							tmpNItem.setInventario(tmpItm.getInventario());
							tmpNItem.setIdRd(generarEntero());
							
							// ...luego iteramos la lista de items que se
							// cotizaron y que contienen los precios y tipos
							// cotizados
							for (CotCmbsItems tmpItmsCot : itemsCotizados) 
							{
								if (tmpItmsCot.getItem().getId().equals(tmpNItem.getId())) {
									
									tmpNItem.setPrecioCotizado(tmpItmsCot.getPrecioCotizado());
									tmpNItem.setTipPreCotizado(tmpItmsCot.getTipoPrecio());
									tmpNItem.setTipoPrecio(tmpItmsCot.getTipoPrecio());
									System.out.println("Encontr� el item y su precio: " + tmpItmsCot.getItem().getProducto().getNombre()+ "  "+ tmpItmsCot.getPrecioCotizado());
									System.out.println("**** Item del combo aparato:  ed "+ tmpNItem);
									preciosCotizados.add(tmpNItem);
									conteoNormal++;
								}
							}
						}
						System.out.println("SiZE de preciosCotizados normal (items): "
								+ preciosCotizados.size());
						// asignamos los precios cotizados
						combo.setItemsCombo(preciosCotizados);
						combo.setDescripcion(tmpCmb.getCombo().getDescripcion());
						combo.setPeriodoGarantia(tmpCmb.getCombo()
								.getPeriodoGarantia());
						combo.setNombre(tmpCmb.getCombo().getNombre());
						combo.setEstado(tmpCmb.getCombo().getEstado());
						combo.setEmpresa(tmpCmb.getCombo().getEmpresa());
						selCmbsList.add(combo);
						System.out.println("*** combo aparato ed " + combo);
						if (combo.getId().equals(cotizacion.getSelComboId())) {
							System.out.println("EL COMBO A VENDER ES: "
									+ combo.getNombre());
							setComboVta(combo);
						}
					}

					// Calculamos los totales

					if (comboVta != null
							&& comboVta.getPeriodoGarantia() != null
							&& comboVta.getPeriodoGarantia() > 0) {
						setTieneGarantia(true);
						System.out.println("# ITEMS DE LA COTIZACION: "
								+ tmpCotiz.getItemsCotizacion().size());
						for (ItemComboApa tmpItm : comboVta.getItemsCombo()) {
							
							ItemComboApa nwItm = new ItemComboApa();
							nwItm.setCantidad(tmpItm.getCantidad());
							nwItm.setCategoria(tmpItm.getCategoria());
							nwItm.setCodProducto(null);
							nwItm.setCombo(comboVta);
							nwItm.setDescripcion(tmpItm.getDescripcion());
							nwItm.setPrincipal(tmpItm.isPrincipal());
							nwItm.setProducto(tmpItm.getProducto());
							nwItm.setTipoPrecio(tmpItm.getTipoPrecio());
							nwItm.setPrecioCotizado(tmpItm.getPrecioCotizado());
							nwItm.setTipPreCotizado(tmpItm.getTipPreCotizado());
							nwItm.setIdRd(generarEntero());
							itemsComboApa.add(nwItm);
						}
					}
				} catch (Exception e) {
					System.err.println(e.getMessage());
					e.printStackTrace();
				}

				System.out.println("Tiene hijo BIN? "
						+ tmpCotiz.getHijoBin().size());
				if (tmpCotiz.getHijoBin() != null
						&& tmpCotiz.getHijoBin().size() > 0) {
					CotizacionComboApa tmpBin = tmpCotiz.getHijoBin().get(0);
					setBinaural(true);
					aparatoClienteHome.getInstance().setLadoAparatoBin(
							tmpBin.getLadoAparato());
					// Cargamos la lista de combos Binaurales cotizados
					// (CotizacionCombos)

					List<CotizacionCombos> tmpCtCmbsBin = getEntityManager()
							.createQuery(
									"SELECT cc FROM CotizacionCombos cc WHERE cc.cotizacion.id = :cotBin")
							.setParameter("cotBin", tmpBin.getId())
							.getResultList();
					System.out.println("Size del tmpCtCmbsBin "
							+ tmpCtCmbsBin.size());
					if (tmpBin.getSelComboId() != null) {
						setSelectedComboVtaBin(tmpBin.getSelComboId());
					}
					try {
						ComboAparato comboBin;// = new ComboAparato(); // nueva linea new
						List<ItemComboApa> preciosCotizadosBin = new ArrayList<ItemComboApa>();int conteoBin=0;
						for (CotizacionCombos tmpCmbBin : tmpCtCmbsBin) //3 combos 3 iteraciones maximo
						{
							itemsCotizadosBin = tmpCmbBin.getItemsCotizados();
							comboBin = new ComboAparato(); //nuevo comentado
							comboBin.setId(tmpCmbBin.getCombo().getId());
							comboBin.setCodigo(tmpCmbBin.getCombo().getCodigo());
							comboBin.setCostosCombo(tmpCmbBin.getCombo()
									.getCostosCombo());
							preciosCotizadosBin = new ArrayList<ItemComboApa>(); // arreglado
							// Vamos a iterar la lista de Items que tiene el
							// combo,
							//ItemComboApa tmpItmBin = new ItemComboApa();
							for (ItemComboApa tmpItmBin : tmpCmbBin.getCombo().getItemsCombo()) //cada combo tiene 2 items 2 iteraciones
							{
								
								ItemComboApa tmpNItem= new ItemComboApa();
								//tmpNItem=tmpItm;
								tmpNItem.setCantidad(tmpItmBin.getCantidad());
								tmpNItem.setCategoria(tmpItmBin.getCategoria());
								tmpNItem.setCodProducto(tmpItmBin.getCodProducto());
								tmpNItem.setCombo(tmpItmBin.getCombo());
								tmpNItem.setDescripcion(tmpItmBin.getDescripcion());
								tmpNItem.setId(tmpItmBin.getId());
								tmpNItem.setProducto(tmpItmBin.getProducto());
								tmpNItem.setPrincipal(tmpItmBin.isPrincipal());
								//tmpNItem.setPrecioCotizado(tmpItm.getPrecioCotizado());
								//tmpNItem.setTipoPrecio(tmpItm.getTipoPrecio());
								//tmpNItem.setTipPreCotizado(tmpItm.getTipPreCotizado());
								tmpNItem.setInventario(tmpItmBin.getInventario());
								tmpNItem.setIdRd(generarEntero());
								
								// ...luego iteramos la lista de items que se
								// cotizaron y que contienen los precios y tipos
								// cotizados
								for (CotCmbsItems tmpItmsCot : itemsCotizadosBin) 
								{
									System.out.println("ID del tmpItmsCot: "+ tmpItmsCot.getItem().getId());
									System.out.println("ID del tmpItm: "+ tmpNItem.getId());
									
									if (tmpItmsCot.getItem().getId().equals(tmpNItem.getId())) 
									{
										tmpNItem.setPrecioCotizado(tmpItmsCot.getPrecioCotizado());
										tmpNItem.setTipPreCotizado(tmpItmsCot.getTipoPrecio());
										tmpNItem.setTipoPrecio(tmpItmsCot.getTipoPrecio());
										System.out.println("Encontr� el item y su precio bin: "+ tmpItmsCot.getItem().getProducto().getNombre()
														+ "  "
														+ tmpItmsCot
																.getPrecioCotizado());
										System.out.println("**** Item del combo aparato binaural:  ed "+ tmpNItem);
										conteoBin++;
										preciosCotizadosBin.add(tmpNItem); // se llena la lista con 2 items
									}
								}
							}
							System.out
									.println("SiZE de preciosCotizados binaural (items): "
											+ preciosCotizadosBin.size());
							// asignamos los precios cotizados
							comboBin.setItemsCombo(preciosCotizadosBin);
							comboBin.setDescripcion(tmpCmbBin.getCombo()
									.getDescripcion());
							comboBin.setPeriodoGarantia(tmpCmbBin.getCombo()
									.getPeriodoGarantia());
							comboBin.setNombre(tmpCmbBin.getCombo().getNombre());
							comboBin.setEstado(tmpCmbBin.getCombo().getEstado());
							comboBin.setEmpresa(tmpCmbBin.getCombo().getEmpresa());
							selCmbsListBin.add(comboBin);
							System.out.println("*** combo aparato binaural ed " + comboBin);
							if (comboBin.getId().equals(tmpBin.getSelComboId())) {
								System.out.println("EL COMBO BIN A VENDER ES: "
										+ comboBin.getNombre());
								setComboVtaBin(comboBin);
							}
							System.out.println("Tama�o selCmbsListBin"
									+ selCmbsListBin.size());
						}
						System.out.println("***************** Agrego a lista de precios # items "+conteoBin);
					} catch (Exception e) {
						System.err.println(e.getMessage()
								+ " entre al catch del load");
						e.printStackTrace();
					}
					if (tmpBin.getPeriodoGarantia() != null
							&& tmpBin.getPeriodoGarantia() > 0)
						setTieneGarantiaBin(true);

					for (ItemComboApa tmpItm : comboVtaBin.getItemsCombo()) {
						ItemComboApa nwItm = new ItemComboApa();
						nwItm.setCantidad(tmpItm.getCantidad());
						nwItm.setCategoria(tmpItm.getCategoria());
						nwItm.setCodProducto(null);
						nwItm.setCombo(comboVtaBin);
						nwItm.setDescripcion(tmpItm.getDescripcion());
						nwItm.setPrincipal(tmpItm.isPrincipal());
						nwItm.setProducto(tmpItm.getProducto());
						nwItm.setTipoPrecio(tmpItm.getTipoPrecio());
						nwItm.setPrecioCotizado(tmpItm.getPrecioCotizado());
						nwItm.setTipPreCotizado(tmpItm.getTipPreCotizado());
						nwItm.setIdRd(generarEntero());
						itemsComboApaBin.add(nwItm);
					}
					setSelBinaural((short) 2);
					recalcularTotalVenta();
					setSelBinaural((short) 1);
				}
				recalcularTotalVenta();
			} else
				setInstance(getEntityManager().find(VentaProdServ.class, vtcId));
		} catch (Exception e) {
			clearInstance();
			setInstance(new VentaProdServ());
		}
		System.out.println("Tamanio de los items cotizados "+ selCmbsList.size());
	}

	public void getCotizacionesPend() {
		if (sucursalFlt == null)
			sucursalFlt = loginUser.getUser().getSucursal();
		cotizacionList = getEntityManager()
				.createQuery(
						"SELECT c FROM CotizacionComboApa c "
								+ "	WHERE c.estado = 'PEN'  AND c.cotizacionComboBin = NULL AND c.sucursal = :suc ORDER BY c.fechaIngreso DESC ")
				.setParameter("suc", sucursalFlt).getResultList();

	}
	
	public void getCotizacionPendByDateN()
	{
		String estado1="PEN";
		String estado2="COT";
		sucursalFlt = loginUser.getUser().getSucursal();
		cotizacionList = getEntityManager()
				.createQuery(
						"SELECT c FROM CotizacionComboApa c "
								//+ "	WHERE ("
								//+ "(c.fechaIngreso BETWEEN :fch1 AND :fch2) AND (c.estado = 'PEN' OR c.estado='COT')  AND c.cotizacionComboBin = NULL AND c.sucursal = :suc ) ORDER BY c.fechaIngreso DESC")
						+ "	WHERE c.fechaIngreso BETWEEN :fch1 AND :fch2 and (c.estado=:estad1 OR c.estado=:estad2) and c.sucursal= :suc and c.cotizacionComboBin=null")
				.setParameter("suc", sucursalFlt)
				.setParameter("estad1", estado1)
				.setParameter("estad2", estado2)
				.setParameter("fch1", getFechaPFlt1())
				.setParameter("fch2", getFechaPFlt2()).getResultList();
		System.out.println(cotizacionList.isEmpty());
		//.setParameter("suc", sucursalFlt)
	}

	public void getCotizacionesPendByDate() {
		cotizacionList.clear();
		
		System.out.println("Entro al evento");
		String fltFch = "(:fch1 = :fch1 OR :fch2 = :fch2) ";
		if (getFechaPFlt1() != null && getFechaPFlt2() != null) {
			setFechaPFlt1(truncDate(getFechaPFlt1(), false));
			setFechaPFlt2(truncDate(getFechaPFlt2(), true));
			fltFch = " (c.fechaIngreso BETWEEN :fch1 AND :fch2) ";
		}
		
		String fltSuc=" AND c.sucursal = :suc";
		//if (sucursalFlt == null)
			sucursalFlt = loginUser.getUser().getSucursal();
		
		
		if(sucursalFlt.getSucursalSuperior()!=null)
		{
			System.out.println("sucursal superior");
			sucursalFlt=sucursalFlt.getSucursalSuperior();
			fltSuc=" AND c.sucursal.sucursalSuperior = :suc";
		}
			
		
		
			cotizacionList = getEntityManager()
					.createQuery(
							"SELECT c FROM CotizacionComboApa c "
									+ "	WHERE ("
									+ fltFch
									+ " OR "
									+
									// "(SELECT array_to_string(array(SELECT
									// cb.combo.nombre FROM CotizacionCombos
									// cb),',')) LIKE :nom OR
									// "UPPER(c.cliente.nombres) LIKE :nom OR UPPER(c.cliente.apellidos) LIKE :nom)" // ORDER BY c.fechaIngreso DESC"
									" UPPER(c.cliente.nombres)||' '||UPPER(c.cliente.apellidos) LIKE :nom)"
									+ "AND (c.estado = 'PEN' OR c.estado='COT')  AND c.cotizacionComboBin = NULL "+fltSuc+" ) ORDER BY c.id DESC")
					.setParameter("suc", sucursalFlt)
					.setParameter("nom", "%" + this.nomCoinci.toUpperCase() + "%")
					.setParameter("fch1", getFechaPFlt1())
					.setParameter("fch2", getFechaPFlt2()).getResultList();
			
			
				System.out.println(cotizacionList.isEmpty());
				System.out.println(getFechaPFlt1().getDate());
				System.out.println(getFechaPFlt2().getDate());
		
	}

	public void getCotiPendByDateAdmin() {
		String fltFch = "(:fch1 = :fch1 OR :fch2 = :fch2) ";
		if (getFechaPFlt1() != null && getFechaPFlt2() != null) {
			setFechaPFlt1(truncDate(getFechaPFlt1(), false));
			setFechaPFlt2(truncDate(getFechaPFlt2(), true));
			fltFch = " (c.fechaIngreso BETWEEN :fch1 AND :fch2) ";
		}
		if (sucursalFlt == null)
			sucursalFlt = loginUser.getUser().getSucursal();
		cotizacionList = getEntityManager()
				.createQuery(
						"SELECT c FROM CotizacionComboApa c "
								+ "	WHERE ("
								+ fltFch
								+ " AND "
								+
								// "(SELECT array_to_string(array(SELECT
								// cb.combo.nombre FROM CotizacionCombos
								// cb),',')) LIKE :nom OR
								" UPPER(c.cliente.nombres) LIKE :nom OR UPPER(c.cliente.apellidos) LIKE :nom)  "
								+ "AND (c.estado = 'PEN' OR c.estado='COT')  AND c.cotizacionComboBin = NULL) ORDER BY c.fechaIngreso DESC")

				.setParameter("nom", "%" + this.nomCoinci.toUpperCase() + "%")
				.setParameter("fch1", getFechaPFlt1())
				.setParameter("fch2", getFechaPFlt2()).getResultList();
	}

	public void fillComboList(ComboAparato combo) {
		System.out.println("Entre a fillComboList SIZE " + selCmbsList.size());
		if (selCmbsList.size() >= 3) {
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_addcmblimit"));
		} else if (selCmbsList.size() < 3) {
			if (selCmbsList.size() > 0) {
				for (ComboAparato tmpCmb : selCmbsList) {
					if (tmpCmb.equals(combo)) {
						FacesMessages.instance().add(Severity.WARN,
								sainv_messages.get("vtaitm_addcmbrepet"));
						return;
					}
				}
			}
			selCmbsList.add(combo);
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_addcmbok"));
		}
	}

	public void fillComboListBin(ComboAparato combo) {
		System.out.println("Entre a fillComboList SIZE "
				+ selCmbsListBin.size());
		if (selCmbsListBin.size() >= 3) {
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_addcmblimit"));
		} else if (selCmbsListBin.size() < 3) {
			if (selCmbsListBin.size() > 0) {
				for (ComboAparato tmpCmb : selCmbsListBin) {
					if (tmpCmb.equals(combo)) {
						FacesMessages.instance().add(Severity.WARN,
								sainv_messages.get("vtaitm_addcmbrepet"));
						return;
					}
				}
			}
			selCmbsListBin.add(combo);
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_addcmbok"));
		}
	}

	public void getVentasItemList() {
		if (sucursalFlt == null)
			sucursalFlt = loginUser.getUser().getSucursal();
		resultList = getEntityManager()
				.createQuery(
						"SELECT v FROM VentaProdServ v WHERE v.tipoVenta = 'CMB' AND v.sucursal = :suc")
				.setParameter("suc", sucursalFlt).getResultList();

		if (loginUser.getUser().getSucursal() == null
				&& (sucursales == null && sucursales.size() <= 0))
			sucursales = getEntityManager().createQuery(
					"SELECT s FROM Sucursal s").getResultList();
	}

	public void setComboVender() {
		setComboVta(null);
		if (getSelectedComboVta() != null && getSelectedComboVta() > 0) {
			System.out.println("Combo seleccionado: " + getSelectedComboVta());
			System.out.println("Numero de combos: " + selCmbsList.size());
			for (ComboAparato ca : selCmbsList) {
				System.out.println("Combo ID: " + ca.getId());
				if (ca.getId().equals(getSelectedComboVta())) {
					System.out.println("Miedo");
					cotizacion.setSelComboId(ca.getId());
					setComboVta(ca);
					if (ca.getPeriodoGarantia() != null
							&& ca.getPeriodoGarantia() > 0) {
						setTieneGarantia(true);
					} else
						setTieneGarantia(false);
					break;
				}
			}
		}
		if (getComboVta() == null) {
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noCombo"));

		} else {
			System.out.println("Entre a setComboVender y comboVta no es null");
			setComboVender(getComboVta());
		}
	

	}
	
	//Nuevo metodo para seleccionar combo binaural
	public void setComboVenderBin() {
		
		setComboVtaBin(null);
		if (getSelectedComboVtaBin() != null && getSelectedComboVtaBin() > 0) {
			System.out.println("Combo seleccionado: "
					+ getSelectedComboVtaBin());
			System.out.println("Numero de combos: " + selCmbsListBin.size());
			for (ComboAparato ca : selCmbsListBin) {
				System.out.println("Combo ID: " + ca.getId());
				if (ca.getId().equals(getSelectedComboVtaBin())) {
					System.out.println("Miedo binaural uyuyuy");
					cotizacion.getHijoBin().get(0).setSelComboId(ca.getId());
					setComboVtaBin(ca);
					if (ca.getPeriodoGarantia() != null
							&& ca.getPeriodoGarantia() > 0) {
						setTieneGarantiaBin(true);
					} else
						setTieneGarantiaBin(false);
					break;
				}
			}
		}
		if (getComboVtaBin() == null) {
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noCombo"));
		} else {
			System.out
					.println("Entre a setComboVender y comboVtaBin no es null");
			setComboVenderBin(getComboVtaBin());
		}
	

	}

	public void setComboVender(ComboAparato cmbApa) {
		System.out.println("Combo aparato seleecionado para vender "
				+ cmbApa.getNombre());
		
			System.err.println("Trata de setear en base al combo aparato");
			setComboVta(cmbApa);
			itemsComboApa = new ArrayList<ItemComboApa>();
			if (cmbApa.getPeriodoGarantia() != null
					&& cmbApa.getPeriodoGarantia() > 0)
				setTieneGarantia(true);
	

		for (ItemComboApa tmpItm : cmbApa.getItemsCombo()) {
			ItemComboApa nwItm = new ItemComboApa();
			System.out.println("Cantidad: " + tmpItm.getCantidad());
			System.out.println("Categoria: "
					+ tmpItm.getCategoria().getNombre());
			System.out.println("Descripcion: " + tmpItm.getDescripcion());
			System.out.println("IsPrincipal: " + tmpItm.isPrincipal());

			nwItm.setCantidad(tmpItm.getCantidad());
			nwItm.setCategoria(tmpItm.getCategoria());
			nwItm.setCodProducto(tmpItm.getCodProducto());
			nwItm.setCombo(cmbApa);
			nwItm.setDescripcion(tmpItm.getDescripcion());
			nwItm.setPrincipal(tmpItm.isPrincipal());
			nwItm.setProducto(tmpItm.getProducto());
			nwItm.setTipoPrecio(tmpItm.getTipoPrecio());
			
				itemsComboApa.add(nwItm);
		}
		calcularPrecios();
	}
	
	
	public void setComboVenderBin(ComboAparato cmbApa) {
		System.out.println("Combo aparato seleecionado para vender "
				+ cmbApa.getNombre());
		
		System.err.println("Trata de setear en base al combo binaural");
		setComboVtaBin(cmbApa);
		itemsComboApaBin = new ArrayList<ItemComboApa>();
		if (cmbApa.getPeriodoGarantia() != null
				&& cmbApa.getPeriodoGarantia() > 0)
			setTieneGarantiaBin(true);
	

		for (ItemComboApa tmpItm : cmbApa.getItemsCombo()) {
			ItemComboApa nwItm = new ItemComboApa();
			System.out.println("Cantidad: " + tmpItm.getCantidad());
			System.out.println("Categoria: "
					+ tmpItm.getCategoria().getNombre());
			System.out.println("Descripcion: " + tmpItm.getDescripcion());
			System.out.println("IsPrincipal: " + tmpItm.isPrincipal());

			nwItm.setCantidad(tmpItm.getCantidad());
			nwItm.setCategoria(tmpItm.getCategoria());
			nwItm.setCodProducto(tmpItm.getCodProducto());
			nwItm.setCombo(cmbApa);
			nwItm.setDescripcion(tmpItm.getDescripcion());
			nwItm.setPrincipal(tmpItm.isPrincipal());
			nwItm.setProducto(tmpItm.getProducto());
			nwItm.setTipoPrecio(tmpItm.getTipoPrecio());
			
			itemsComboApaBin.add(nwItm);
		}
		calcularPrecios();
	}

	public void selItemFromCat(ItemComboApa itm) {
		selItmCombo = itm;
		productoHome.cargarListaProdsCat(itm.getCategoria(), null);
	}

	public void setProductoItm(Inventario inv) {
		selItmCombo.setProducto(inv.getProducto());
		selItmCombo.setInventario(inv);
		selItmCombo = null;
		recalcularTotalVenta();
	}

	public void agregarProducto(Inventario prd) {
		ventaItemHome.agregarProducto(prd);
		recalcularTotalVenta();
	}

	public void actualizarSubtotalPrd() {
		for (Item tmpItm : ventaItemHome.getItemsAgregados()){
			if (tmpItm.getTipoPrecio().equals("NRM"))
				tmpItm.setCostoUnitario(tmpItm.getInventario().getProducto().getPrcNormal());
			else if (tmpItm.getTipoPrecio().equals("OFE"))
				tmpItm.setCostoUnitario(tmpItm.getInventario().getProducto().getPrcOferta());
			else if (tmpItm.getTipoPrecio().equals("MIN"))
				tmpItm.setCostoUnitario(tmpItm.getInventario().getProducto().getPrcMinimo());
		}
		ventaItemHome.actualizarSubtotal();
		recalcularTotalVenta();
	}

	public void removerItemPrd(Item i) {
		ventaItemHome.removerItem(i);
		recalcularTotalVenta();
	}

	public void agregarServicio(Service srv) {
		ventaItemHome.agregarServicio(srv);
		recalcularTotalVenta();
	}

	public void actualizarSubtotalSrv() {
		ventaItemHome.actualizarSubtotalSrv();
		recalcularTotalVenta();
	}

	public void removerServicio(Service i) {
		ventaItemHome.removerServicio(i);
		recalcularTotalVenta();
	}

	public void selCodForItem(ItemComboApa itm) {
		selItmCombo = new ItemComboApa();
		selItmCombo = itm;
		cargarListaCodigos(itm);
		// Si esta el binaural, se filtra uqe no salgan los codigos que ya
		// fueron seleccionados en el otro aparato
		/*List<ItemComboApa> tmpLstItm = new ArrayList<ItemComboApa>();
		if (isBinaural() && selBinaural == 1)
			tmpLstItm = itemsComboApaBin;
		else if (isBinaural() && selBinaural == 2)
			tmpLstItm = itemsComboApa;

		for (ItemComboApa tmpItm : tmpLstItm) {
			if (tmpItm.getCodProducto() != null)
				if (codsProducto.contains(tmpItm.getCodProducto()))
					codsProducto.remove(tmpItm.getCodProducto());
		}*/
		
		if (codsProducto.contains(codCombo1))
			codsProducto.remove(codCombo1);
		
	}
	
	public void selCodForItemBin(ItemComboApa itm) {
		selItmCombo = new ItemComboApa();
		selItmCombo = itm;
		cargarListaCodigos(itm);
		// Si esta el binaural, se filtra uqe no salgan los codigos que ya
		// fueron seleccionados en el otro aparato
		/*List<ItemComboApa> tmpLstItm = new ArrayList<ItemComboApa>();
		if (isBinaural() && selBinaural == 1)
			tmpLstItm = itemsComboApaBin;
		else if (isBinaural() && selBinaural == 2)
			tmpLstItm = itemsComboApa;

		for (ItemComboApa tmpItm : tmpLstItm) {
			if (tmpItm.getCodProducto() != null)
				if (codsProducto.contains(tmpItm.getCodProducto()))
					codsProducto.remove(tmpItm.getCodProducto());
		}*/
		
		if (codsProducto.contains(codCombo2))
			codsProducto.remove(codCombo2);
		
	}

	public void setCodigosItm(CodProducto cdp) {
		List<ItemComboApa> tmpItems = new ArrayList<ItemComboApa>();
		codCombo1= new CodProducto();
		boolean flag = false;
		if (comboVta == null) {
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noCombo"));
			return;
		}/* else if (isBinaural() && selBinaural != 1 && comboVtaBin == null) {
			FacesMessages.instance().clear();
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noComboBin"));
			return;
		} else if (selBinaural == 1 && comboVta != null
				&& comboVta.getItemsCombo().size() > 0) {*/
			for (ItemComboApa tmpItm : comboVta.getItemsCombo()) {
				if (tmpItm == selItmCombo) {
					//tmpItm.setCodProducto(cdp);
					codCombo1=cdp;
					System.out.println("Agregu� el codigo al item: "
							+ tmpItm.getProducto().getNombre());
					
					flag= true;
				}
				tmpItems.add(tmpItm);
			}
			for(ItemComboApa tmpItm:comboVta.getItemsCombo()){
				System.out.println("Id del item: " + tmpItm.getId() );
				System.out.println("Item: "+tmpItm.getProducto().getNombre());
				System.out.println("Num serie: "+tmpItm.getCodProducto());
			}
			if (flag == true ){
				comboVta.setItemsCombo(tmpItems);
				itemsComboApa= tmpItems;
				//selItmCombo.setCodProducto(cdp);
				selItmCombo = null;
				tmpItems = new ArrayList<ItemComboApa>();	
			}
			else {
				FacesMessages.instance().clear();
				FacesMessages.instance().add(Severity.WARN,
						sainv_messages.get("vtaitm_error_IncCombo"));
				return;
			}
		//}
	}
	
	public void setCodigosItmBin(CodProducto cdp){
		List<ItemComboApa> tmpItems = new ArrayList<ItemComboApa>();
		codCombo2= new CodProducto();
		boolean flag = false;
		if (isBinaural() && selBinaural != 1 && comboVtaBin == null) {
			FacesMessages.instance().clear();
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noComboBin"));
			return;
		}/*else if (selBinaural == 2 && comboVtaBin != null
				&& comboVtaBin.getItemsCombo().size() > 0) {*/
			for (ItemComboApa tmpItm : comboVtaBin.getItemsCombo()) {
				if (tmpItm == selItmCombo) {
					//tmpItm.setCodProducto(cdp);
					codCombo2=cdp;
					System.out.println("Agregu� el codigo al item Bin: "
							+ tmpItm.getProducto().getNombre());
					
					flag=true;
				}
				tmpItems.add(tmpItm);
			}
			for(ItemComboApa tmpItm:comboVtaBin.getItemsCombo()){
				System.out.println("Id del item: " + tmpItm.getId() );
				System.out.println("Item Bin: "+tmpItm.getProducto().getNombre());
				System.out.println("Num serie: "+tmpItm.getCodProducto());
			}
		//}
		if (flag == true ){
			comboVtaBin.setItemsCombo(tmpItems);
			itemsComboApaBin= tmpItems;
			//selItmCombo.setCodProducto(cdp);
			selItmCombo = null;
			tmpItems = new ArrayList<ItemComboApa>();	
		}
		else {
			FacesMessages.instance().clear();
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_IncCombo"));
			return;
		}

		
	}

	public void cargarListaCodigos(ItemComboApa prdItm) {
		Inventario inv = (Inventario) getEntityManager()
				.createQuery(
						"SELECT i FROM Inventario i WHERE i.sucursal = :suc AND i.producto = :prd")
				.setParameter("suc", loginUser.getUser().getSucursal())
				.setParameter("prd", prdItm.getProducto()).getSingleResult();

		// Sacamos el inventario del producto
		codsProducto = (ArrayList<CodProducto>) getEntityManager()
				.createQuery(
						"SELECT c FROM CodProducto c "
								+ "	WHERE c.inventario.producto = :prd AND c.inventario = :inv AND c.estado = 'ACT' ")
				.setParameter("prd", prdItm.getProducto())
				.setParameter("inv", inv).getResultList();
	}

	/***** APARATO BINAURAL METODOS ******/

	public void setApaBinaural() {
		setBinaural(true);
		setSelBinaural((short) 1);
		if (aparatoClienteHome.getInstance().getLadoAparato().equals("IZQ"))
			aparatoClienteHome.getInstance().setLadoAparatoBin("DER");
		else
			aparatoClienteHome.getInstance().setLadoAparatoBin("IZQ");
	}

	public void quitApaBinaural() {
		setBinaural(false);
		setSelBinaural((short) 1);
		/*
		 * setComboVtaBin(null); setItemsComboApaBin(new
		 * ArrayList<ItemComboApa>());
		 */
	}

	public void cambiarComboSel(Integer numApa) {
		setSelBinaural(numApa.shortValue());
	}

	public void actualizarLadoApa() {
		if (selBinaural == 1 && binaural) {
			if (aparatoClienteHome.getInstance().getLadoAparato().equals("IZQ"))
				aparatoClienteHome.getInstance().setLadoAparatoBin("DER");
			else
				aparatoClienteHome.getInstance().setLadoAparatoBin("IZQ");
		} else if (selBinaural == 2 && binaural) {
			if (aparatoClienteHome.getInstance().getLadoAparatoBin()
					.equals("IZQ"))
				aparatoClienteHome.getInstance().setLadoAparato("DER");
			else
				aparatoClienteHome.getInstance().setLadoAparato("IZQ");
		}

	}

	public String obtenerLado(CotizacionComboApa cot) {
		if (cot.getHijoBin() != null && cot.getHijoBin().size() > 0) {
			return "Binaural";
		} else if (cot.getLadoAparato().equals("DER"))
			return "O�do derecho";
		else
			return "O�do izquierdo";
	}

	/***** METODO QUE PERSISTE EN BASE DE DATOS LOS TIPOS DE PRECIO QUE SE HAYAN ASIGNADO A LOS ITEMS DE LOS COMBOS E ITEMS ADICIONALES ******/
	public boolean actualizarCotizacion() {
		try {
			actualizarPreCotizados();
			// Actualizamos los combos cotizados
			if (cotizacion.getCmbCotizados() != null
					&& cotizacion.getCmbCotizados().size() > 0) {
				for (CotizacionCombos cc : cotizacion.getCmbCotizados()) {
					System.out.println("Combos cotizados "
							+ cc.getCombo().getNombre() + " Cotizacion: "
							+ cc.getCotizacion().getId());
					for (ComboAparato ca : selCmbsList) {
						if (cc.getCombo().getId() == ca.getId()) {
							System.out
									.println("Encontre el combo, seria de actualizarlo");
							List<CotCmbsItems> itemsCot = new ArrayList<CotCmbsItems>();
							for (ItemComboApa tmpitem : ca.getItemsCombo()) {
								System.out
										.println("Tipos de precio que van dentro de ca: "
												+ tmpitem.getTipoPrecio());
								CotCmbsItems itemCot = new CotCmbsItems();
								Integer id = (Integer) getEntityManager()
										.createQuery(
												"SELECT cci.id FROM CotCmbsItems cci WHERE cci.ctCmbs = :ctCmbs AND cci.item = :item")
										.setParameter("ctCmbs", cc)
										.setParameter("item", tmpitem)
										.getSingleResult();
								getEntityManager().flush();
								System.out
										.println("ID DEL CotCmbsItems: " + id);
								itemCot.setId(id);
								itemCot.setCtCmbs(cc);
								itemCot.setPrecioCotizado(tmpitem
										.getPrecioCotizado());
								itemCot.setTipoPrecio(tmpitem
										.getTipPreCotizado());
								itemCot.setItem(tmpitem);
								itemsCot.add(itemCot);
								getEntityManager().merge(itemCot);
								getEntityManager().flush();

							}
							cc.setItemsCotizados(itemsCot);
						}
					}
					if (cotizacion.getHijoBin()!=null && cotizacion.getHijoBin().size() > 0){
					CotizacionComboApa cotBin = cotizacion.getHijoBin().get(0);
					for (CotizacionCombos ccBin : cotBin.getCmbCotizados()){
						for (ComboAparato ca : selCmbsListBin) {
							if (ccBin.getCombo().getId() == ca.getId()) {
								System.out
										.println("Encontre el comboBin, seria de actualizarlo");
								List<CotCmbsItems> itemsCot = new ArrayList<CotCmbsItems>();
								for (ItemComboApa tmpitem : ca.getItemsCombo()) {
									System.out
											.println("Tipos de precio que van dentro de caBin: "
													+ tmpitem.getTipoPrecio());
									CotCmbsItems itemCot = new CotCmbsItems();
									Integer id = (Integer) getEntityManager()
											.createQuery(
													"SELECT cci.id FROM CotCmbsItems cci WHERE cci.ctCmbs = :ctCmbs AND cci.item = :item")
											.setParameter("ctCmbs", ccBin)
											.setParameter("item", tmpitem)
											.getSingleResult();
									getEntityManager().flush();
									System.out
											.println("ID DEL CotCmbsItemsBin: " + id);
									itemCot.setId(id);
									itemCot.setCtCmbs(ccBin);
									itemCot.setPrecioCotizado(tmpitem
											.getPrecioCotizado());
									itemCot.setTipoPrecio(tmpitem
											.getTipPreCotizado());
									itemCot.setItem(tmpitem);
									itemsCot.add(itemCot);
									getEntityManager().merge(itemCot);
									getEntityManager().flush();
	
								}
								ccBin.setItemsCotizados(itemsCot);
							}
						}
					}
					}	
				}
				FacesMessages.instance().add(Severity.WARN,
						sainv_messages.get("cotitm_update"));
			}
			for (ComboAparato ca : selCmbsList) {
				// System.out.println("Tratando de actualizar combo Bin: " +
				// ca.getCodigo());
				for (ItemComboApa ic : ca.getItemsCombo()) {
					System.out.println("	Item: " + ic.getProducto().getNombre()
							+ "Nuevo tipo precio: " + ic.getTipoPrecio());
				}
			}
			if (ventaItemHome.getItemsAgregados() != null && ventaItemHome.getItemsAgregados().size() > 0){
				System.out.println("VentaItemHome.ItemsAgregados tiene items");
				for (Item tmpItm:ventaItemHome.getItemsAgregados()){
					for (CotizacionPrdSvcAdicionales tmpCot : cotizacion.getCotizItmSvcAdi()){
						if(tmpCot.getId().equals(tmpItm.getCotItmId())){
							System.out.println("Voy a actualizar los precios (items adicionales)");
							System.out.println("Precio cotizado: "+ tmpItm.getCostoUnitario());
							tmpCot.setPrecioCotizado(tmpItm.getCostoUnitario());
							tmpCot.setTipPreCotizado(tmpItm.getTipoPrecio());
							System.out.println(" Tipo precio: "+ tmpCot.getTipPreCotizado());
							getEntityManager().merge(tmpCot);
							getEntityManager().flush();
						}
					}
				}
				
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/***** APARATO BINAURAL METODOS ******/

	public boolean cotizarVenta() {
		if (selCmbsList.isEmpty()){
			nombreCompleto=instance.getCliente();
			instance.setCliente(nombreCompleto);
			
			FacesMessages.instance().clear();
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_save3"));
			return false;
		}else if (isBinaural() && selCmbsListBin.isEmpty()){
			nombreCompleto=instance.getCliente();
			instance.setCliente(nombreCompleto);
			FacesMessages.instance().clear();
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_save2"));
			return false;
		}
		
		instance.setSucursal(loginUser.getUser().getSucursal());
		List<CotizacionComboItem> itemsCotizacion = new ArrayList<CotizacionComboItem>();
		// Guardamos la cotizacion y su detalle
		if (instance.getSucursal() == null) {
			nombreCompleto=instance.getCliente();
			instance.setCliente(nombreCompleto);
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_save1"));
			return false;
		}

		if (instance.getCliente() == null) {
			nombreCompleto=instance.getCliente();
			instance.setCliente(nombreCompleto);
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_saveb"));
			return false;
		}

		CotizacionComboApa cot = genCotizacionApa(false, null);

		List<CotizacionCombos> ctcmbs = genCotizacionCombos(cot, selCmbsList);
		for (CotizacionCombos tmpCtCmbs : ctcmbs) {
			getEntityManager().persist(tmpCtCmbs);
			System.out.println("Persist� el combo con ID: "
					+ tmpCtCmbs.getCombo().getNombre());
			getEntityManager().flush();
			for (ItemComboApa itm : tmpCtCmbs.getCombo().getItemsCombo()) {
				CotCmbsItems ctcmit = new CotCmbsItems();
				ctcmit.setItem(itm);
				ctcmit.setCtCmbs(tmpCtCmbs);
				ctcmit.setTipoPrecio(itm.getTipoPrecio());
				if (itm != null) {
					if (itm.getProducto()!=null && itm.getTipoPrecio().equals("NRM"))
						ctcmit.setPrecioCotizado(itm.getProducto().getPrcNormal());
					else if (itm.getProducto()!=null && itm.getTipoPrecio().equals("MIN"))
						ctcmit.setPrecioCotizado(itm.getProducto().getPrcMinimo());
					else if (itm.getProducto()!=null && itm.getTipoPrecio().equals("OFE"))
						ctcmit.setPrecioCotizado(itm.getProducto().getPrcOferta());
					else{
						FacesMessages.instance().clear();
						FacesMessages.instance().add(Severity.WARN,
								sainv_messages.get("cotitm_calctot_faltitm2"));
						return false;
					}
				}
				getEntityManager().persist(ctcmit);
				getEntityManager().flush();
			}
		}
		// Armamos la cotizacion de Servicios adicionales al combo agregados
		if (ventaItemHome.getServiciosAgregados().size() > 0){
			for (Service srv : ventaItemHome.getServiciosAgregados()) {
				//llenos la cotizacion de servicios adicionales
				CotizacionPrdSvcAdicionales tmpCot = new CotizacionPrdSvcAdicionales();
				tmpCot.setServicio(srv);
				tmpCot.setCantidad(srv.getCantidad());
				tmpCot.setPrecioCotizado(srv.getCosto().floatValue());
				tmpCot.setTipPreCotizado("NRM");
				tmpCot.setCotizacion(cot);
				prdSvcAdicionales.add(tmpCot);
			}
		}
		
		// Armamos la cotizacion de Productos adicionales al combo agregados
		if (ventaItemHome.getItemsAgregados().size() > 0){
			for (Item item : ventaItemHome.getItemsAgregados()) {
				CotizacionPrdSvcAdicionales tmpCot = new CotizacionPrdSvcAdicionales();
				tmpCot.setCantidad(item.getCantidad());
				tmpCot.setCotizacion(cot);
				if (item.getInventario().getProducto()!=null)
					tmpCot.setProducto(item.getInventario().getProducto());
				if (item.getInventario().getProducto() != null) {
					if (item.getTipoPrecio().equals("NRM")) {
						tmpCot.setPrecioCotizado(item.getInventario().getProducto().getPrcNormal());
						tmpCot.setTipPreCotizado("NRM");
					} else if (item.getTipoPrecio().equals("MIN")) {
						tmpCot.setPrecioCotizado(item.getInventario().getProducto().getPrcMinimo());
						tmpCot.setTipPreCotizado("MIN");
					} else if (item.getTipoPrecio().equals("OFE")) {
						tmpCot.setPrecioCotizado(item.getInventario().getProducto().getPrcOferta());
						tmpCot.setTipPreCotizado("OFE");
					}
				}
				prdSvcAdicionales.add(tmpCot);
			}
		}
		//persistimos la lista de cotizacion de productos o servicios adicioanles
		if (ventaItemHome.getItemsAgregados().size() > 0 || ventaItemHome.getServiciosAgregados().size() > 0){
			for (CotizacionPrdSvcAdicionales tmpCot:prdSvcAdicionales){
				getEntityManager().persist(tmpCot);
				getEntityManager().flush();
			}
		}
		
		CotizacionComboApa cotBin;
		if (cot == null)
			return false;

		if (isBinaural()) {
			cotBin = genCotizacionApa(true, cot);
			List<CotizacionCombos> ctcmbsBin = genCotizacionCombos(cotBin,
					selCmbsListBin);
			for (CotizacionCombos tmpCtCmbsBin : ctcmbsBin) {
				getEntityManager().persist(tmpCtCmbsBin);
				System.out.println("Persist� el comboBin con ID: "
						+ tmpCtCmbsBin.getCombo().getNombre());
				getEntityManager().flush();
				for (ItemComboApa itm : tmpCtCmbsBin.getCombo().getItemsCombo()) {
					CotCmbsItems ctcmit = new CotCmbsItems();
					ctcmit.setItem(itm);
					ctcmit.setCtCmbs(tmpCtCmbsBin);
					ctcmit.setTipoPrecio(itm.getTipoPrecio());
					if (itm != null) {
						if (itm.getTipoPrecio().equals("NRM"))
							ctcmit.setPrecioCotizado(itm.getProducto()
									.getPrcNormal());
						else if (itm.getTipoPrecio().equals("MIN"))
							ctcmit.setPrecioCotizado(itm.getProducto()
									.getPrcMinimo());
						else if (itm.getTipoPrecio().equals("OFE"))
							ctcmit.setPrecioCotizado(itm.getProducto()
									.getPrcOferta());
					}
					getEntityManager().persist(ctcmit);
					getEntityManager().flush();
				}
			}
		} else {
			setComboVtaBin(null);
			setItemsComboApaBin(new ArrayList<ItemComboApa>());
		}
		FacesMessages.instance().clear();
		FacesMessages.instance().add(
				sainv_messages.get("vtaitm_succ_cotizexit"));
		return true;
	}

	private List<CotizacionCombos> genCotizacionCombos(
			CotizacionComboApa cotizacion, List<ComboAparato> combos) {
		List<CotizacionCombos> tmpCtCmbs = new ArrayList<CotizacionCombos>();
		CotizacionCombos cb;
		for (ComboAparato tmpCmb : combos) {
			cb = new CotizacionCombos();
			cb.setCombo(tmpCmb);
			cb.setCotizacion(cotizacion);
			tmpCtCmbs.add(cb);
			System.out.println("Se guardo el combo: " + tmpCmb.getNombre());
		}
		return tmpCtCmbs;
	}

	private CotizacionComboApa genCotizacionApa(boolean esBinaural,
			CotizacionComboApa cotizPadre) {
		List<CotizacionComboItem> itemsCotizacion = new ArrayList<CotizacionComboItem>();
		List<ItemComboApa> tmpLstIt;
		if (esBinaural) {
			System.out.println("Entre al if thus esBinaural true");
			tmpLstIt = itemsComboApaBin;
		} else
			tmpLstIt = itemsComboApa;
		System.out.println("Tama�o de tmpLstIt dentro de genCotizacionApa "
				+ tmpLstIt.size());
		for (ItemComboApa tmpItm : tmpLstIt) {
			if (tmpItm.getProducto() == null) {
				FacesMessages.instance().add(
						sainv_messages.get("vtaitm_error_noitm"));
				System.out.println("primero");
				return null;
			}

			Inventario currInv = (Inventario) getEntityManager()
					.createQuery(
							"SELECT i FROM Inventario i WHERE i.producto = :prd AND i.sucursal = :suc")
					.setParameter("prd", tmpItm.getProducto())
					.setParameter("suc", instance.getSucursal())
					.getSingleResult();
			/*
			 * if(currInv.getCantidadActual() == 0) {
			 * FacesMessages.instance().add(
			 * sainv_messages.get("vtaitm_error_noexisvta")); return null; }
			 */
			tmpItm.setInventario(currInv);

			CotizacionComboItem itm = new CotizacionComboItem();
			itm.setCantidad(tmpItm.getCantidad());
			itm.setCategoria(tmpItm.getCategoria());
			itm.setDescripcion(tmpItm.getDescripcion());
			itm.setInventario(tmpItm.getInventario());
			itm.setTipoPrecio(tmpItm.getTipoPrecio());
			if (tmpItm.getTipoPrecio().equals("NRM"))
				itm.setMonto(tmpItm.getProducto().getPrcNormal().doubleValue());
			else if (tmpItm.getTipoPrecio().equals("MIN"))
				itm.setMonto(tmpItm.getProducto().getPrcMinimo().doubleValue());
			else if (tmpItm.getTipoPrecio().equals("OFE"))
				itm.setMonto(tmpItm.getProducto().getPrcOferta().doubleValue());
			itm.setPrincipal(tmpItm.isPrincipal());
			itm.setProducto(tmpItm.getProducto());
			System.out.println("Longitud de items cotizacion antes de add: " + itemsCotizacion.size());
			itemsCotizacion.add(itm);
			System.out.println("Longitud de items cotizacion despues de add: " + itemsCotizacion.size());
		}
		CotizacionComboApa cotizacion = new CotizacionComboApa();
		cotizacion.setCliente(instance.getCliente());
		cotizacion.setValidez(getValidez());
		if (esBinaural) {
			cotizacion.setCombo(comboVtaBin);
			if (tieneGarantiaBin)
				cotizacion.setPeriodoGarantia(comboVtaBin.getPeriodoGarantia());
		} else {
			cotizacion.setCombo(comboVta);
			if (tieneGarantia)
				cotizacion.setPeriodoGarantia(comboVta.getPeriodoGarantia());
		}
		cotizacion.setDetalleAparato(instance.getDetalle());
		cotizacion.setEstado("PEN");
		cotizacion.setFechaIngreso(new Date());
		if (esBinaural)
			cotizacion.setLadoAparato(aparatoClienteHome.getInstance()
					.getLadoAparatoBin());
		else
			cotizacion.setLadoAparato(aparatoClienteHome.getInstance()
					.getLadoAparato());

		cotizacion.setRetroAuricular(aparatoClienteHome.getInstance()
				.isRetroAuricular());
		cotizacion.setSucursal(loginUser.getUser().getSucursal());
		cotizacion.setUsuarioGenera(loginUser.getUser());

		if (cotizPadre != null)
			cotizacion.setCotizacionComboBin(cotizPadre);

		getEntityManager().persist(cotizacion);
		getEntityManager().refresh(cotizacion);
		// Persistimos los hijos formados previamente
		for (CotizacionComboItem tmpItm : itemsCotizacion) {
			tmpItm.setCotizacion(cotizacion);
			getEntityManager().persist(tmpItm);
			System.out.println("Si estoy entrando");
		}
		getEntityManager().refresh(cotizacion);
		return cotizacion;
	}
	
	
	//Este opcion se incluira si el remanente es cero
	public boolean finalizarVentaPorCredito()
	{
		
		
		if(comboVta!=null)
		{
			ReparacionCliente rpCl=(ReparacionCliente) getEntityManager().createQuery("SELECT r FROM ReparacionCliente r where r.cliente=:idCliente and r.idCot=:idCot and r.tipoRep='CRE'")
					.setParameter("idCliente",instance.getCliente())
					.setParameter("idCot", cotizacion.getId()).getSingleResult();
			
			if(rpCl.getEstado().equals("PEN"))
			{
								
				FacesMessages.instance().add(Severity.WARN,
						"Existe un trabajo de taller pendiente");

				return false;
			}
				
		}
		
		if(comboVtaBin!=null)
		{
			
			ReparacionCliente rpCl=(ReparacionCliente) getEntityManager().createQuery("SELECT r FROM ReparacionCliente r where r.cliente=:idCliente and r.idCot=:idCot and r.tipoRep='CRE'")
					.setParameter("idCliente",instance.getCliente())
					.setParameter("idCot", cotizacion.getHijoBin().get(0).getId()).getResultList().get(0);
			
			if(rpCl.getEstado().equals("PEN"))
			{
								
				FacesMessages.instance().add(Severity.WARN,
						"Existe un trabajo de taller pendiente");

				return false;
			}
			
		}
		
		
		if(!preSave())
			return false;
		//
		
		
		
		//Se registra la venta
		instance.setEstado("ABF");
		instance.setMonto(0f);
		getEntityManager().persist(instance);
		
		
		// Generamos el movimiento del inventario
		Movimiento mov = new Movimiento();
		mov.setFecha(new Date());
		mov.setSucursal(loginUser.getUser().getSucursal());
		mov.setTipoMovimiento("S");
		mov.setUsuario(loginUser.getUser());
		mov.setRazon("V");
		mov.setObservacion("Salida por la venta generada automaticamente de un combo de aparato, detalle de la venta:"
				+ instance.getDetalle());
		
		//getEntityManager().persist(mov);
		//getEntityManager().flush();
		
		Map<Integer, ItemComboApa> lstProdsMov = new HashMap<Integer, ItemComboApa>(20);
		//Map<Integer, ItemComboApa> lstProdsMovBin = new HashMap<Integer, ItemComboApa>(20);
		Set<Integer> prdsRep = new HashSet<Integer>();
		
		//Para el combo seleccionado de la izquierda
		AparatoCliente apaCli = aparatoClienteHome.getInstance();
		
		if(comboVta!=null)
		{
			System.out.println("*** Entro al comboVTa items "+itemsComboApa.size());
			for (ItemComboApa tmpItm : itemsComboApa) 
			{
				
				
				if (!lstProdsMov.containsKey(tmpItm.getProducto().getId())) {
					
					tmpItm.setCantidad((short) 1);
					//tmpItm.setPrecioCotizado();
					lstProdsMov.put(tmpItm.getProducto().getId(), tmpItm);
					
				} else {
					short qty = lstProdsMov.get(tmpItm.getProducto().getId()).getCantidad();
					lstProdsMov.get(tmpItm.getProducto().getId()).setCantidad(++qty);
				}
				
				System.out.println("inventsrio nombre o id "+ tmpItm.getInventario().getId());

				Item item = new Item();
				item.setCantidad(1);
				item.setInventario(tmpItm.getInventario());
				item.setItemId(new ItemId());
				item.getItemId().setInventarioId(tmpItm.getInventario().getId());
				item.getItemId().setMovimientoId(mov.getId());
				item.setMovimiento(mov);
				
				//item.setCostoUnitario(tmpItm.getPrecioCotizado());//????
				item.setCostoUnitario(tmpItm.getProducto().getCosto());//???
				item.setPrecioCotizado(tmpItm.getPrecioCotizado());//??
				item.setPrecioVenta(tmpItm.getPrecioCotizado());//??
				
				//item.setCodProducto(itemCmb.getCodProducto());
				if(codCombo1!=null)
					item.setCodProducto(codCombo1);
				
				item.setTipoPrecio(tmpItm.getTipPreCotizado());
				
				
				
				System.out.println("For");
				if(tmpItm.isPrincipal())
				{
					System.out.println("*** Entro a es principal");
					
					//Si requiere molde o ensamblaje el producto ya fue registrado al cliente pero en estado PNP= pendiente de pago
					if(tmpItm.getCategoria().getReqMolde()==true || tmpItm.getCategoria().getCategoriaPadre().getReqMolde()==true || 
							tmpItm.getCategoria().getReqEnsamble()==true || tmpItm.getCategoria().getCategoriaPadre().getReqEnsamble()==true)
					{
						System.out.println("*** aparato requiere molde o ensamblaje");
						
						/*//Seleccionar el producto que se registro al cliente para su reparacion idProducto, idCotizacion, estado=pnp  . Posiblemente haya mas de un aparatp registrado en reparacion del mismo producto, pero solo queremos uno de la lista
						ReparacionCliente rpCl=(ReparacionCliente) getEntityManager().createQuery("SELECT r FROM ReparacionCliente r where r.cliente=:idCliente and r.idCot=:idCot and r.tipoRep='CRE'")
								.setParameter("idCliente",instance.getCliente())
								.setParameter("idCot", cotizacion.getId()).getSingleResult();
						*/
						
						apaCli=(AparatoCliente) getEntityManager().createQuery("SELECT r.aparatoRep FROM ReparacionCliente r where r.cliente=:idCliente and r.idCot=:idCot and r.tipoRep='CRE'")
																.setParameter("idCliente",instance.getCliente())
																.setParameter("idCot", cotizacion.getId()).getSingleResult();
						 
						
						/*if(rpCl.getEstado().equals("PEN"))
						{
							
							FacesMessages.instance().add(Severity.WARN,
									"Existe un trabajo de taller pendiente");
							
							return false;
						}*/
						
						
						//apaCli=rpCl.getAparatoRep();
						
						
						
																
						System.out.println("*** aparato extraido "+ "Estado=" +apaCli.getEstado() + "IdAparato" +apaCli.getId());
						
						if (tmpItm.getCategoria().isTieneNumLote()) 
						{
							//Asignar el numero de lote al producto que se registro en la reparacion, idProducto,idCotizacion,estado= pnp
							//apaCli.setNumLote(tmpItm.getCodProducto().getNumLote());
							apaCli.setNumLote(codCombo1.getNumLote());
							apaCli.setEstado("ACT"); // Se actualiza el estado del aparato del cliente
							
							//tmpItm.getCodProducto().setEstado("USD"); // se desabilita el numero de lote
							codCombo1.setEstado("USD");
							
							//getEntityManager().merge(tmpItm.getCodProducto());
							getEntityManager().merge(codCombo1);
							getEntityManager().merge(apaCli);
							getEntityManager().flush();
							
							System.out.println("*** entro a numero de lote");
							
						}
						if (tmpItm.getCategoria().isTieneNumSerie()) 
						{
							item.setCodsSerie(codCombo1.getNumSerie());//Item 
							
							//apaCli.setNumSerie(tmpItm.getCodProducto().getNumSerie());
							apaCli.setNumSerie(codCombo1.getNumSerie());
							apaCli.setEstado("ACT");

							//tmpItm.getCodProducto().setEstado("USD");
							codCombo1.setEstado("USD");
							
							//getEntityManager().merge(tmpItm.getCodProducto());
							getEntityManager().merge(codCombo1);
							getEntityManager().merge(apaCli);
							getEntityManager().flush();
							
							System.out.println("*** entro a numero de serie");
						}
						
						
						
						
					}
					else //si el aparato no requiere molde ni ensamblaje el producto no ha sido registrado al cliente
					{
						System.out.println("*** No requiere trabajo de taller");
						//Iniciar el registro del aparato al cliente,
						//Asignarle el numero de seria o lote
						//Reducir del inventario los numeros de serie/lote y el producto seleccionado
						//Generar la venta con estado abono finalizado, agregar el codigo de la cotizacion en descripcion
						
						apaCli.setCliente(instance.getCliente());
						apaCli.setActivo(true);
						apaCli.setCostoVenta(getTotalCostos());
						apaCli.setCustomApa(false);
						apaCli.setEstado("ACT");
						if (tieneGarantia)
							apaCli.setPeriodoGarantia(comboVta.getPeriodoGarantia());
						apaCli.setFechaAdquisicion(new Date());
						apaCli.setHechoMedida(false);
						apaCli.setNombre(comboVta.getNombre());
						
						if (tmpItm.getCategoria().isTieneNumLote()) 
						{
							//apaCli.setNumLote(tmpItm.getCodProducto().getNumLote());
							apaCli.setNumLote(codCombo1.getNumLote());
							//tmpItm.getCodProducto().setEstado("USD"); // se desabilita el numero de lote
							codCombo1.setEstado("USD");
							
							//getEntityManager().merge(tmpItm.getCodProducto());
							getEntityManager().merge(codCombo1);
							
							
						}
						if (tmpItm.getCategoria().isTieneNumSerie()) 
						{
							item.setCodsSerie(codCombo1.getNumSerie());//item del movimiento
							//apaCli.setNumSerie(tmpItm.getCodProducto().getNumSerie());
							apaCli.setNumSerie(codCombo1.getNumSerie());
							
							//tmpItm.getCodProducto().setEstado("USD"); // se desabilita el numero de serie
							codCombo1.setEstado("USD");
							
							//getEntityManager().merge(tmpItm.getCodProducto());
							getEntityManager().merge(codCombo1);
							
							
						}
						
						getEntityManager().persist(apaCli); //Se registra el aparato al cliente
						
					}
					
					
					
					
				}
				
				//Reduciendo de inventario los productos del combo
				
				Inventario inveProd=(Inventario) getEntityManager()
						.createQuery("Select i From Inventario i where i.producto=:productoCom and i.sucursal=:sucursalCom")
						.setParameter("productoCom",tmpItm.getProducto())
						.setParameter("sucursalCom", cotizacion.getSucursal()) //La cotizacion tiene datos? 
						.getSingleResult();
				
				inveProd.setCantidadActual(inveProd.getCantidadActual()-1);
				getEntityManager().merge(inveProd);
				
				System.out.println("item actual " + tmpItm.getProducto().getNombre());
				
				// Generamos el detalle de la venta
				DetVentaProdServ detVta = new DetVentaProdServ();
				detVta.setCantidad(tmpItm.getCantidad().intValue());
				detVta.setMonto(tmpItm.getPrecioCotizado());
				detVta.setVenta(instance);
				detVta.setCodExacto(tmpItm.getInventario().getProducto()
						.getReferencia());
				detVta.setCodClasifVta("PRD");
				detVta.setCosto(tmpItm.getInventario().getProducto().getCosto());
				detVta.setProducto(tmpItm.getProducto());
				
				if(tmpItm.isPrincipal())
				{
					//if (tmpItm.getCodProducto() != null && tmpItm.getCodProducto().getNumSerie() != null)
					if (codCombo1 != null && codCombo1.getNumSerie() != null)
						detVta.setNumSerie(codCombo1.getNumSerie());//detVta.setNumSerie(tmpItm.getCodProducto().getNumSerie());
					
					//if (tmpItm.getCodProducto() != null && tmpItm.getCodProducto().getNumLote() != null)
					if (codCombo1 != null && codCombo1.getNumLote() != null)
						detVta.setNumLote(codCombo1.getNumLote());//detVta.setNumLote(tmpItm.getCodProducto().getNumLote());
				}

				// Formamos el detalle
				StringBuilder bld = new StringBuilder();
				bld.append(tmpItm.getInventario().getProducto().getNombre());
				
				if (tmpItm.getInventario().getProducto().getModelo() != null)
					bld.append(", Modelo "
							+ tmpItm.getInventario().getProducto().getModelo());
				if (tmpItm.getInventario().getProducto().getMarca() != null)
					bld.append(", Marca "
							+ tmpItm.getInventario().getProducto().getMarca()
									.getNombre());
				detVta.setDetalle(bld.toString());
				
				detVta.setCombo(comboVta);
				detVta.setCodCoti(cotizacion.getId());
				
				getEntityManager().persist(detVta);
				
				//getEntityManager().persist(item);//item del movimiento
				
				//getEntityManager().flush();
	
			}//Fin for
			

				
			
			//Servicios del combo
		    for (CostoServicio srv : comboVta.getCostosCombo()) {
		    	
			    totalservprod+=srv.getServicio().getCosto().floatValue();
				DetVentaProdServ dtVta = new DetVentaProdServ();
				dtVta.setCantidad(1);
				StringBuilder bld = new StringBuilder();
				bld.append(srv.getServicio().getName());
				dtVta.setDetalle(bld.toString());
				dtVta.setEscondido(true);
				dtVta.setMonto(srv.getServicio().getCosto().floatValue());
				dtVta.setVenta(instance);
				dtVta.setCodClasifVta("SRV");
				dtVta.setCodExacto(srv.getServicio().getCodigo());
				dtVta.setServicio(srv.getServicio());
				getEntityManager().persist(dtVta); //Descomentado el 29/11/2016
			}
			
			
			
			for (Integer idPrd : lstProdsMov.keySet()) {
				
				Item item = new Item();
				item.setCantidad(lstProdsMov.get(idPrd).getCantidad()
						.intValue());
				item.setInventario(lstProdsMov.get(idPrd).getInventario());
				item.setItemId(new ItemId());
				item.getItemId().setInventarioId(item.getInventario().getId());
				
				//item.setPrecioCotizado(lstProdsMov.get(idPrd).getPrecioCotizado()); se cambio el 06/12/2016
				item.setCostoUnitario(lstProdsMov.get(idPrd).getProducto().getCosto());
				item.setPrecioCotizado(lstProdsMov.get(idPrd).getPrecioCotizado());
				item.setPrecioVenta(lstProdsMov.get(idPrd).getPrecioCotizado());
				
				
				if (lstProdsMov.keySet().contains(idPrd)) {
					Integer cantAct = item.getCantidad();
					cantAct += lstProdsMov.get(idPrd).getCantidad();
					item.setCantidad(cantAct);
					prdsRep.add(idPrd);
				}
				movimientoHome.getItemsAgregados().add(item);
			}
			
			
			/*DetVentaProdServ detVta = new DetVentaProdServ();
			detVta.setCantidad(1);
			//detVta.setMonto( +totalservprod);
			detVta.setMonto(totalCostos);
			totalservprod = 0f;
			// detVta.setMonto(0f);
			detVta.setVenta(instance);
			detVta.setCodExacto(comboVta.getCodigo());
			detVta.setCodClasifVta("CMB");
			// Formamos el detalle
			StringBuilder bld = new StringBuilder();
			bld.append("COMBO " + comboVta.getNombre());

			detVta.setDetalle(bld.toString());
		
			
			//nuevo 09/11/2016
			detVta.setCombo(comboVta);
			detVta.setCodCoti(cotizacion.getId());
		
			for(ItemComboApa itm:itemsComboApa)
			{
				if(itm.isPrincipal())
				{
					System.out.println("*** asignar serie");
					if(itm.getCategoria().isTieneNumLote())
						detVta.setNumLote(codCombo1.getNumLote());
					
					if(itm.getCategoria().isTieneNumSerie())
						detVta.setNumSerie(codCombo1.getNumSerie());
				}
				
			}
			
			getEntityManager().persist(detVta);*/
			
			//Actualizar estado cotizacion 
			cotizacion.setEstado("APL");
			cotizacion.setFechaVenta(new Date());
			cotizacion.setIdVta(instance);
			getEntityManager().merge(cotizacion);
			
			
		}
		
		//Para el combo seleccionado de la derecha
		AparatoCliente apaCliBin = new AparatoCliente();
		if(comboVtaBin!=null)
		{
			CotizacionComboApa cotizacionBin = cotizacion.getHijoBin().get(0);
			for (ItemComboApa tmpItm : itemsComboApaBin) 
			{
				
				
				
				
				Item itemBin = new Item();
				itemBin.setCantidad(1);
				itemBin.setInventario(tmpItm.getInventario());
				itemBin.setItemId(new ItemId());
				itemBin.getItemId().setInventarioId(
				tmpItm.getInventario().getId());
				itemBin.getItemId().setMovimientoId(mov.getId());
				itemBin.setMovimiento(mov);
				
				//itemBin.setCostoUnitario(tmpItm.getPrecioCotizado());
				itemBin.setCostoUnitario(tmpItm.getProducto().getCosto());
				itemBin.setPrecioCotizado(tmpItm.getPrecioCotizado()); //??????
				itemBin.setPrecioVenta(tmpItm.getPrecioCotizado());//?????
				
				if(tmpItm.isPrincipal())
				{
					
					//Si requiere molde o ensamblaje el producto ya fue registrado al cliente pero en estado PNP= pendiente de pago
					if(tmpItm.getCategoria().getReqMolde()==true || tmpItm.getCategoria().getCategoriaPadre().getReqMolde()==true || 
							tmpItm.getCategoria().getReqEnsamble()==true || tmpItm.getCategoria().getCategoriaPadre().getReqEnsamble()==true)
					{
						
						//Seleccionar el producto que se registro al cliente para su reparacion idProducto, idCotizacion, estado=pnp  . Posiblemente haya mas de un aparatp registrado en reparacion del mismo producto, pero solo queremos uno de la lista
						apaCliBin=(AparatoCliente) getEntityManager().createQuery("SELECT r.aparatoRep FROM ReparacionCliente r where r.cliente=:idCliente and r.idCot=:idCot and r.tipoRep='CRE'")
								.setParameter("idCliente",instance.getCliente())
								.setParameter("idCot", cotizacionBin.getId()).getSingleResult();
																
						
						if (tmpItm.getCategoria().isTieneNumLote()) 
						{
							//Asignar el numero de lote al producto que se registro en la reparacion, idProducto,idCotizacion,estado= pnp
							//apaCliBin.setNumLote(tmpItm.getCodProducto().getNumLote());
							apaCliBin.setNumLote(codCombo2.getNumLote());
							apaCliBin.setEstado("ACT"); // Se actualiza el estado del aparato del cliente
							
							//tmpItm.getCodProducto().setEstado("USD"); // se desabilita el numero de lote
							codCombo2.setEstado("USD");
							
							//getEntityManager().merge(tmpItm.getCodProducto());
							getEntityManager().merge(codCombo2);
							getEntityManager().merge(apaCliBin);
							getEntityManager().flush();
							
							
						}
						if (tmpItm.getCategoria().isTieneNumSerie()) 
						{
							
							itemBin.setCodsSerie(codCombo2.getNumSerie());//item del movimiento
							//apaCliBin.setNumSerie(tmpItm.getCodProducto().getNumSerie());
							apaCliBin.setNumSerie(codCombo2.getNumSerie());
							apaCliBin.setEstado("ACT");

							//tmpItm.getCodProducto().setEstado("USD");
							
							codCombo2.setEstado("USD");
							//tmpItm.setCodProducto(codCombo2);
							
							//getEntityManager().merge(tmpItm.getCodProducto());
							getEntityManager().merge(codCombo2);
							getEntityManager().merge(apaCliBin);
							getEntityManager().flush();
						}
						
						
						
						
					}
					else //si el aparato no requiere molde ni ensamblaje el producto no ha sido registrado al cliente
					{
						
						//Iniciar el registro del aparato al cliente,
						//Asignarle el numero de seria o lote
						//Reducir del inventario los numeros de serie/lote y el producto seleccionado
						//Generar la venta con estado abono finalizado, agregar el codigo de la cotizacion en descripcion
						
						apaCliBin.setCliente(cotizacionBin.getCliente());
						apaCliBin.setActivo(true);
						apaCliBin.setCostoVenta(getTotalCostos());
						apaCliBin.setCustomApa(false);
						apaCliBin.setEstado("ACT");
						if (tieneGarantiaBin)
							apaCliBin.setPeriodoGarantia(comboVtaBin.getPeriodoGarantia());
						apaCliBin.setFechaAdquisicion(new Date());
						apaCliBin.setHechoMedida(false);
						apaCliBin.setNombre(comboVtaBin.getNombre());
						apaCliBin.setLadoAparatoBin(apaCli.getLadoAparatoBin());
						apaCliBin.setRetroAuricular(apaCli.isRetroAuricular());
						apaCliBin.setDetalleAparato(apaCli.getDetalleAparato());
						
						if (tmpItm.getCategoria().isTieneNumLote()) 
						{
							//apaCliBin.setNumLote(tmpItm.getCodProducto().getNumLote());
							apaCliBin.setNumLote(codCombo2.getNumLote());
							
							codCombo2.setEstado("USD");
							
							getEntityManager().merge(codCombo2);
							
						}
						
						if (tmpItm.getCategoria().isTieneNumSerie()) 
						{
							itemBin.setCodsSerie(codCombo2.getNumSerie());//item del movimiento
							//apaCliBin.setNumSerie(tmpItm.getCodProducto().getNumSerie());
							apaCliBin.setNumSerie(codCombo2.getNumSerie());
							
							codCombo2.setEstado("USD");
							getEntityManager().merge(codCombo2);
							
						}
						
						getEntityManager().persist(apaCliBin); //Se registra el aparato al cliente
						
					}
					
					
					
					
				}
				
				//Reduciendo de inventario los productos del combo
				
				Inventario inveProd=(Inventario) getEntityManager()
						.createQuery("Select i From Inventario i where i.producto=:productoCom and i.sucursal=:sucursalCom")
						.setParameter("productoCom",tmpItm.getProducto())
						.setParameter("sucursalCom", cotizacion.getSucursal()) //La cotizacion tiene datos? 
						.getSingleResult();
				
				inveProd.setCantidadActual(inveProd.getCantidadActual()-1);
				getEntityManager().merge(inveProd);
				
				
				// Generamos el detalle de la venta
				DetVentaProdServ detVta = new DetVentaProdServ();
				detVta.setCantidad(tmpItm.getCantidad().intValue());
				detVta.setMonto(tmpItm.getPrecioCotizado());
				detVta.setVenta(instance);
				detVta.setCodExacto(tmpItm.getInventario().getProducto()
						.getReferencia());
				detVta.setCodClasifVta("PRD");
				detVta.setCosto(tmpItm.getInventario().getProducto().getCosto());
				detVta.setProducto(tmpItm.getProducto());

				if (tmpItm.getCodProducto() != null
						&& tmpItm.getCodProducto().getNumSerie() != null)
					detVta.setNumSerie(tmpItm.getCodProducto().getNumSerie());
				if (tmpItm.getCodProducto() != null
						&& tmpItm.getCodProducto().getNumLote() != null)
					detVta.setNumLote(tmpItm.getCodProducto().getNumLote());
				
				if(tmpItm.isPrincipal())
				{	
					if (codCombo2 != null
							&& codCombo2.getNumSerie() != null)
						detVta.setNumSerie(codCombo2.getNumSerie());
					if (codCombo2 != null
							&& codCombo2.getNumLote() != null)
						detVta.setNumLote(codCombo2.getNumLote());
				}
				
				
				// Formamos el detalle
				StringBuilder bld = new StringBuilder();
				bld.append(tmpItm.getInventario().getProducto().getNombre());
				if (tmpItm.getInventario().getProducto().getModelo() != null)
					bld.append(", Modelo "
							+ tmpItm.getInventario().getProducto().getModelo());
				if (tmpItm.getInventario().getProducto().getMarca() != null)
					bld.append(", Marca "
							+ tmpItm.getInventario().getProducto().getMarca()
									.getNombre());
				detVta.setDetalle(bld.toString());
				
				detVta.setCombo(comboVtaBin);
				
				//24/11/2016
				detVta.setCodCoti(cotizacionBin.getId());
				
				getEntityManager().persist(detVta);
				
				//getEntityManager().persist(itemBin);//item del movimiento
				
				
				if (!lstProdsMov.containsKey(tmpItm.getProducto()
						.getId())) {
					lstProdsMov.put(tmpItm.getProducto().getId(),
							tmpItm);
					tmpItm.setCantidad((short) 1);
				} else {
					short qty = lstProdsMov.get(
							tmpItm.getProducto().getId()).getCantidad();
					lstProdsMov.get(tmpItm.getProducto().getId())
							.setCantidad(++qty);
				}
	
			}//Fin for
			
			
			
			// Servicios del combo
			 for (CostoServicio srv : comboVtaBin.getCostosCombo()) {
				DetVentaProdServ dtVta = new DetVentaProdServ();
				dtVta.setCantidad(1);
				StringBuilder bld = new StringBuilder();
				bld.append(srv.getServicio().getName());
				dtVta.setDetalle(bld.toString());
				dtVta.setEscondido(true);
				dtVta.setMonto(srv.getServicio().getCosto().floatValue());
				dtVta.setVenta(instance);
				dtVta.setCodExacto(srv.getServicio().getCodigo());
				dtVta.setCodClasifVta("SRV");
				dtVta.setServicio(srv.getServicio());
				getEntityManager().persist(dtVta);
			}
			
			
			/*DetVentaProdServ detVta = new DetVentaProdServ();
			detVta.setCantidad(1);
			//detVta.setMonto( +totalservprod);
			detVta.setMonto(totalCostosBin);
			//totalservprod = 0f;
			// detVta.setMonto(0f);
			detVta.setVenta(instance);
			detVta.setCodExacto(comboVtaBin.getCodigo());
			detVta.setCodClasifVta("CMB");
			// Formamos el detalle
			StringBuilder bld = new StringBuilder();
			bld.append("COMBO " + comboVta.getNombre());

			detVta.setDetalle(bld.toString());
		
			
			//nuevo 09/11/2016
			detVta.setCombo(comboVtaBin);
			
			//24/11/2016
			detVta.setCodCoti(cotizacionBin.getId());
		
			for(ItemComboApa itm:itemsComboApaBin)
			{
				if(itm.isPrincipal())
				{
					System.out.println("*** asignar serie");
					if(itm.getCategoria().isTieneNumLote())
						detVta.setNumLote(codCombo2.getNumLote());
					
					if(itm.getCategoria().isTieneNumSerie())
						detVta.setNumSerie(codCombo2.getNumSerie());
				}
				
			}
			
			getEntityManager().persist(detVta);*/
			
				//Actualizamos el estado de la cotizacion
				
			cotizacionBin.setEstado("APL");
			cotizacionBin.setIdVta(instance);
			cotizacionBin.setFechaVenta(new Date());
			getEntityManager().merge(cotizacionBin);
				
			cotizacion.setEstado("APL");
			cotizacion.setFechaVenta(new Date());
			cotizacion.setIdVta(instance);
			getEntityManager().merge(cotizacion);
			
		}
		
		for (Integer idPrd : lstProdsMov.keySet()) {
			
			
			if (!prdsRep.contains(idPrd)) {
				Item item = new Item();
				item.setCantidad(lstProdsMov.get(idPrd).getCantidad()
						.intValue());
				item.setInventario(lstProdsMov.get(idPrd)
						.getInventario());
				
				item.setCostoUnitario(lstProdsMov.get(idPrd).getProducto().getCosto());
				item.setPrecioCotizado(lstProdsMov.get(idPrd).getPrecioCotizado());
				item.setPrecioVenta(lstProdsMov.get(idPrd).getPrecioCotizado());
				
				//item.setCodsSerie(lstProdsMov.get(idPrd).getCodProducto().getNumSerie());
				
				item.setItemId(new ItemId());
				item.getItemId().setInventarioId(
						item.getInventario().getId());
				movimientoHome.getItemsAgregados().add(item);
			}
		}
		
		movimientoHome.select(mov);
		movimientoHome.save();
		
		//getEntityManager().flush();
		
		FacesMessages.instance().add(Severity.INFO,
				"Datos actualizados, la venta ha sido generada");
		
		return true;
		
		
	}

	@Override
	public boolean preSave() {
		/*if (comboVta==null){
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noCombo"));
			return false;
		}*/ /*else if((selBinaural==2 ||isBinaural()) && comboVtaBin==null){
			System.out.println("Mensaje falta seleccion");
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noComboBin"));
			return false;
		}*/
		
		if (comboVta==null && comboVtaBin==null){
			FacesMessages.instance().add(Severity.WARN,
					"Debe seleccionar al menos un combo");
			return false;
		}
		
		
		
		
		
		instance.setEstado("PEN");
		instance.setMonto(getSubTotal());
		instance.setIdDetalle(0);
		instance.setTipoVenta("CMB");
		instance.setSucursal(loginUser.getUser().getSucursal());
		instance.setEmpresa(loginUser.getUser().getSucursal().getEmpresa());
		instance.setFechaVenta(new Date());
		instance.setUsrEfectua(loginUser.getUser());
		instance.setCliente(cotizacion.getCliente());
		instance.setCotizacion(cotizacion);

		if ((ventaItemHome.getItemsAgregados() != null && ventaItemHome
				.getItemsAgregados().size() > 0)
				|| (ventaItemHome.getServiciosAgregados() != null && ventaItemHome
						.getServiciosAgregados().size() > 0)) {
			ventaItemHome.setInstance(new VentaProdServ());
			ventaItemHome.getInstance().setSucursal(instance.getSucursal());
			ventaItemHome.getInstance().setCliente(instance.getCliente());

			if (!ventaItemHome.preSave())
				return false;
		}

		if (instance.getSucursal() == null) {
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_save1"));
			return false;
		}

		if (instance.getCliente() == null) {
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_saveb"));
			return false;
		}

		if (!isBinaural()) {
			setComboVtaBin(null);
			setItemsComboApaBin(new ArrayList<ItemComboApa>());
			System.out.println("Acabo de setear ComboVtaBin a null");
		}
		
		
		//codigoPrueba
		if(isBinaural() && comboVtaBin==null)
		{
			setComboVtaBin(null);
			setItemsComboApaBin(new ArrayList<ItemComboApa>());
			System.out.println("Acabo de setear ComboVtaBin a null");
			
		}

		Map<Inventario, Integer> invItemsUsed = new HashMap<Inventario, Integer>();

		// Validamos que si lleva categorias de producto el combo, hayan
		// seleccionado un producto especifico y
		// que si llevan numero de serie o de lote que lo hayan seleccionado
		
		if(comboVta!=null)
		{
			for (ItemComboApa tmpItm : itemsComboApa) {
				if (tmpItm.getProducto() == null) {
					FacesMessages.instance().add(
							sainv_messages.get("vtaitm_error_noitm"));
					System.out.println("Segundo");
					return false;
				}
	
				
				
				//if (tmpItm.getCategoria().isTieneNumLote() && (tmpItm.getCodProducto() == null || tmpItm.getCodProducto().getNumLote() == null))
				if (tmpItm.getCategoria().isTieneNumLote() && (codCombo1 == null))
				{
					FacesMessages.instance().add(
							sainv_messages.get("vtaitm_error_itmnolot"));
					System.out.println("Item sin n�mero de lote: "+tmpItm.getProducto().getNombre());
					return false;
				}
				
				
				//if (tmpItm.getCategoria().isTieneNumSerie() && (tmpItm.getCodProducto() == null || tmpItm.getCodProducto().getNumSerie() == null))
				if (tmpItm.getCategoria().isTieneNumSerie() && (codCombo1==null))
				{
					FacesMessages.instance().add(
							sainv_messages.get("vtaitm_error_itmnoser"));
					System.out.println("Item sin n�mero de serie: "+tmpItm.getProducto().getNombre());
					return false;
				}
	
				Inventario currInv = (Inventario) getEntityManager()
						.createQuery(
								"SELECT i FROM Inventario i WHERE i.producto = :prd AND i.sucursal = :suc")
						.setParameter("prd", tmpItm.getProducto())
						.setParameter("suc", instance.getSucursal())
						.getSingleResult();
	
				if (invItemsUsed.get(currInv) == null)
					invItemsUsed.put(currInv, 1);
				else
					invItemsUsed.put(currInv,
							invItemsUsed.get(currInv).intValue() + 1);
	
				tmpItm.setInventario(currInv);
			}
		}

		if (isBinaural() && comboVtaBin!=null)
			for (ItemComboApa tmpItm : itemsComboApaBin) {
				if (tmpItm.getProducto() == null) {
					FacesMessages.instance().add(
							sainv_messages.get("vtaitm_error_noitm"));
					System.out.println("Tercero");
					return false;
				}

				//if (tmpItm.getCategoria().isTieneNumLote() && (tmpItm.getCodProducto() == null || tmpItm.getCodProducto().getNumLote() == null)) 
				if (tmpItm.getCategoria().isTieneNumLote() && (codCombo2==null))
				{
					FacesMessages.instance().add(
							sainv_messages.get("vtaitm_error_itmnolot"));
					System.out.println("Item sin n�mero de lote: "+tmpItm.getProducto().getNombre());
					return false;
				}
				
				
				//if (tmpItm.getCategoria().isTieneNumSerie() && (tmpItm.getCodProducto() == null || tmpItm.getCodProducto().getNumSerie() == null)) 
				if (tmpItm.getCategoria().isTieneNumSerie() && (codCombo2==null))
				{
					FacesMessages.instance().add(
							sainv_messages.get("vtaitm_error_itmnoser"));
					System.out.println("Item sin n�mero de serie: "+tmpItm.getProducto().getNombre());
					return false;
				}

				Inventario currInv = (Inventario) getEntityManager()
						.createQuery(
								"SELECT i FROM Inventario i WHERE i.producto = :prd AND i.sucursal = :suc")
						.setParameter("prd", tmpItm.getProducto())
						.setParameter("suc", instance.getSucursal())
						.getSingleResult();

				if (invItemsUsed.get(currInv) == null)
					invItemsUsed.put(currInv, 1);
				else
					invItemsUsed.put(currInv, invItemsUsed.get(currInv)
							.intValue() + 1);

				tmpItm.setInventario(currInv);
			}

		for (Inventario usdInv : invItemsUsed.keySet()) {
			if (usdInv.getCantidadActual() < invItemsUsed.get(usdInv)) {
				FacesMessages.instance().add(Severity.WARN,
						sainv_messages.get("vtaitm_error_noexisvta"));
				return false;
			}
		}

		aparatoClienteHome.getInstance().setDetalleAparato(
				instance.getDetalle());
		instance.setDetalle("Venta de combo de aparato - "
				+ instance.getDetalle());
		recalcularTotalVenta();
		if (totalCostosBin == null)
			totalCostosBin = 0f;
		
		if(comboVta!=null && comboVtaBin==null)
			instance.setMonto(totalCostos);
		else if(comboVta==null && comboVtaBin!=null)
			instance.setMonto(totalCostosBin);
		else
			instance.setMonto(totalCostos + totalCostosBin);
		

		if (ventaItemHome.getItemsAgregados() != null
				&& ventaItemHome.getItemsAgregados().size() > 0)
			instance.setMonto(instance.getMonto() + ventaItemHome.getSubTotal());

		if (ventaItemHome.getServiciosAgregados() != null
				&& ventaItemHome.getServiciosAgregados().size() > 0)
			instance.setMonto(instance.getMonto()
					+ ventaItemHome.getSubTotalSrv());
		return true;
	}

	public boolean cancelarCotizacion() {
		cotizacion.setEstado("CAN");
		getEntityManager().merge(cotizacion);
		if (cotizacion.getCotizacionComboBin() != null) {
			cotizacion.getCotizacionComboBin().setEstado("CAN");
			getEntityManager().merge(cotizacion.getCotizacionComboBin());
		}
		FacesMessages.instance().clear();
		FacesMessages.instance()
				.add(sainv_messages.get("vtaitm_succ_cotizcan"));

		return true;
	}

	public void calcularPrecios() {
		Float totalItems = 0f, totalFinal = 0f;
		try {
			if (selCmbsList != null && selCmbsList.size() > 0) {
				// iteramos por cada combo de la lista normal
				for (ComboAparato ca : selCmbsList) {
					for (ItemComboApa item : ca.getItemsCombo()) {
						if (item.getProducto() == null) {
							FacesMessages.instance().add(Severity.ERROR,sainv_messages.get("cotitm_calctot_faltitm"));
							return;
						}
					}
					ca.setTotal(0.0f); // reiniciamos el total a 0 para forzar recalculacion
					ca.getTotal(); // hacemos que se calcule 
				}
				if (selCmbsListBin != null && selCmbsListBin.size() > 0) {
					// iteramos por cada combo de la lista binaural
					for (ComboAparato ca : selCmbsListBin) {
						for(ItemComboApa item:ca.getItemsCombo()){
							if (item.getProducto() == null) {
								FacesMessages.instance().add(Severity.ERROR,sainv_messages.get("cotitm_calctot_faltitm"));
								return;
							}
						}
						ca.setTotal(0.0f); // reiniciamos el total a 0 para forzar recalculacion
						ca.getTotal(); // hacemos que se calcule
					}
				}
				setShowTotal(true);
				if (cotizacion != null) {
					recalcularTotalVenta();

				}
			}
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			FacesMessages.instance().add(Severity.ERROR,
					sainv_messages.get("cotitm_calctot_faltitm"));
			// Si se dio este error volvemos a poner los totales a 0 para que no
			// se desplieguen en la pantalla
			if (selCmbsList != null && selCmbsList.size() > 0) {
				for (ComboAparato ca : selCmbsList) {
					ca.setTotal(0.0f);
				}
			}
			if (selCmbsListBin != null && selCmbsListBin.size() > 0) {
				for (ComboAparato ca : selCmbsListBin) {
					ca.setTotal(0.0f);
				}
			}
		} catch (Exception e) {

		}

	}

	public float calcPreReporte(ComboAparato combo) {
		combo.setTotal(0.0f); // reiniciamos el total a 0 para forzar
								// recalculacion
		return combo.getTotal(); // hacemos que se calcule
	}

	public String calcGarantia(ComboAparato combo) {
		switch (combo.getPeriodoGarantia()) {
		case 366:
			return "1 a�o";
		case 30:
			return "1 mes";
		default:
			if (combo.getPeriodoGarantia() % 366 == 0) {
				return combo.getPeriodoGarantia() % 366 + " a�os";
			} else if (combo.getPeriodoGarantia() % 30 == 0) {
				return combo.getPeriodoGarantia() % 30 + " meses";
			} else {
				return combo.getPeriodoGarantia() + " dias";
			}
		}
	}

	public void porCobrar() {
		/*if (comboVta==null){
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noCombo"));
			return;
		} else if((selBinaural==2 ||isBinaural()) && comboVtaBin==null){
			FacesMessages.instance().add(Severity.WARN,
					sainv_messages.get("vtaitm_error_noComboBin"));
			return;
		}*/
		
		if(comboVta==null && comboVtaBin==null){
			FacesMessages.instance().add(Severity.WARN,"Selecciona al menos un combo");
			return;
		}
		
		int defaultTime = 15;
		String resumen = "ART�CULOS:";
		CuentaCobrar cxc = new CuentaCobrar();
		cxc.setMonto(new Float(totalMagnanime));
		cxc.setCliente(cotizacion.getCliente());
		cxc.setComprobante("COT" + cotizacion.getId());
		conceptoMovHome.setConcepto("COTIZACION PEDIDA A PLAZOS");
		conceptoMovHome.guardarConcepto();
		cxc.setConcepto(conceptoMovHome.getInstance());
		CondicionPago cndPg = (CondicionPago) getEntityManager()
				.createQuery(
						"SELECT c FROM CondicionPago c WHERE UPPER(c.nombre) = UPPER(:nom)")
				.setParameter("nom", "EFECTIVO").getSingleResult();
		cxc.setCondicionPago(cndPg);
		cxc.setDiasPlazo(defaultTime);
		cxc.setFechaIngreso(new Date());
		cxc.setSucursal(cotizacion.getSucursal());
		for (ItemComboApa ica : itemsComboApa) {
			resumen += ica.getCantidad() + " " + ica.getProducto().getNombre()
					+ "||";
		}
		for (ItemComboApa ica : itemsComboApaBin) {
			resumen += ica.getCantidad() + " " + ica.getProducto().getNombre()
					+ "||";
		}
		if (resumen.length() > 300) {
			resumen = resumen.substring(0, 300);
		}
		cxc.setComentario(resumen);
		cxc.setRemanente(cxc.getMonto().floatValue());
		cuentaCobrarHome.select(cxc);
		cuentaCobrarHome.save();
		cotizacion.setCuentaCobrar(cxc);
		cotizacion.setEstado("COT"); 
		cotizacion.setFechaCredito(new Date());	
		cotizacion.setFechaVenta(new Date());
										// estado cotizado usado para cotizaciones
										// a las que se les ha generado
										// una cuenta por cobrar pero no se ha
										// procedido a realizar la venta.
		
		//Aqui agregar el trabajo de taller
		
		AparatoCliente apaCli = aparatoClienteHome.getInstance();
		
		if(comboVta!=null)
		{
			//apaCli.setCliente(instance.getCliente());
			apaCli.setCliente(cotizacion.getCliente());
			apaCli.setActivo(true);
			apaCli.setCostoVenta(getTotalCostos());
			apaCli.setCustomApa(false);
			apaCli.setEstado("PNP");
			if (tieneGarantia)
				apaCli.setPeriodoGarantia(comboVta.getPeriodoGarantia());
			apaCli.setFechaAdquisicion(new Date());
			apaCli.setHechoMedida(false);
			apaCli.setNombre(comboVta.getNombre());
			
			
			////////// Nuevo codigo para agregar orden de laboratorio por cada aparato si asi lo requieren
				for (ItemComboApa itemCmb : itemsComboApa)
				{
				
					if(itemCmb.isPrincipal())
					{
						
						apaCli.setModelo(itemCmb.getProducto().getModelo());
						apaCli.setMarca(itemCmb.getProducto().getMarca().getNombre());
						apaCli.setIdPrd(itemCmb.getProducto().getId());
						
						
						System.out.println("Es principal 1 ");
							//Si requiere elaboracion de molde, se realiza la orden de laboratorio
							if(itemCmb.getProducto().getCategoria().getReqMolde()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqMolde()==true)
							{
								
								getEntityManager().persist(apaCli);
								
								System.out.println("�ntro a molde 1");
								ReparacionCliente repCli = new ReparacionCliente();
								repCli.setAprobada(true);
								repCli.setAparatoRep(apaCli);
								repCli.setCliente(apaCli.getCliente());
								repCli.setDescripcion("Elaboracion de molde apartir de venta de aparato a credito");
								repCli.setTipoRep("CRE");
								
								//Obteniendo sucursal
								if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
									repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
								else
									repCli.setSucursal(loginUser.getUser().getSucursal());
								
								
								repCli.setFechaIngreso(new Date());
								repCli.setEstado("PEN");
								repCli.setIdCot(getCotizId());
								
								ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Fabricacion de molde'").getSingleResult();
								
								repCli.setProceso(procesoTall);
								
								getEntityManager().persist(repCli);
								
								//Luego de guardar la reparacion, generamos todas las etapas
								List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
											"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
											.setParameter("proceso", procesoTall).getResultList();
								
								EtapaRepCliente etRepCli = null; 
								Calendar fechaEst = new GregorianCalendar();
								fechaEst.setTime(repCli.getFechaIngreso());
								boolean isFirst = true;
								for(EtapaReparacion etapaRep: lstEtapas){
									etRepCli = new EtapaRepCliente();
									if(isFirst) { //Seteamos la primera 
										isFirst = false;
										etRepCli.setEstado("PEN");
										Calendar cal = new GregorianCalendar();
									//	cal.add(Calendar.DATE, 1);
										etRepCli.setFechaInicio(cal.getTime());
									}
									etRepCli.setEtapaRep(etapaRep);
									etRepCli.setReparacionCli(repCli);
									etRepCli.setUsuario(loginUser.getUser());
									//Calculamos la fecha estimada de finalizacion de cada etapa
									fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
									etRepCli.setFechaEstFin(fechaEst.getTime());
									etRepCli.setReparacionCli(repCli);
									getEntityManager().persist(etRepCli);
								    getEntityManager().flush();
								}
								
							}
							
							//Si requiere ensamblaje se realiza la orden
							if(itemCmb.getProducto().getCategoria().getReqEnsamble()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqEnsamble()==true)
							{
								
								getEntityManager().persist(apaCli);
								
								System.out.println("Entro a ensamblaje 1");
								ReparacionCliente repCli = new ReparacionCliente();
								repCli.setAprobada(true);
								repCli.setAparatoRep(apaCli);
								repCli.setCliente(apaCli.getCliente());
								repCli.setDescripcion("Orden de ensamblaje de aparato apartir de venta de aparato a credito");
								repCli.setTipoRep("CRE");
								repCli.setIdCot(getCotizId());
								
								//Obteniendo sucursal
								if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
									repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
								else
									repCli.setSucursal(loginUser.getUser().getSucursal());
								
								
								repCli.setFechaIngreso(new Date());
								repCli.setEstado("PEN");
								
								ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Ensamblaje de aparato'").getSingleResult();
								
								repCli.setProceso(procesoTall);
								
								getEntityManager().persist(repCli);
								
								//Luego de guardar la reparacion, generamos todas las etapas
								List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
											"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
											.setParameter("proceso", procesoTall).getResultList();
								
								EtapaRepCliente etRepCli = null; 
								Calendar fechaEst = new GregorianCalendar();
								fechaEst.setTime(repCli.getFechaIngreso());
								boolean isFirst = true;
								for(EtapaReparacion etapaRep: lstEtapas){
									etRepCli = new EtapaRepCliente();
									if(isFirst) { //Seteamos la primera 
										isFirst = false;
										etRepCli.setEstado("PEN");
										Calendar cal = new GregorianCalendar();
									//	cal.add(Calendar.DATE, 1);
										etRepCli.setFechaInicio(cal.getTime());
									}
									etRepCli.setEtapaRep(etapaRep);
									etRepCli.setReparacionCli(repCli);
									etRepCli.setUsuario(loginUser.getUser());
									//Calculamos la fecha estimada de finalizacion de cada etapa
									fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
									etRepCli.setFechaEstFin(fechaEst.getTime());
									etRepCli.setReparacionCli(repCli);
									getEntityManager().persist(etRepCli);
								    getEntityManager().flush();
								}
								
							}
						
						
						
					}
			}
		} ////////////////////////////
		
		
		
		//si las categorias requieren molde o ensamblaje de ejecuta el siguiente codigo
		
		
				//Generamos la orden para el aparato binaural
			
				if (isBinaural() && comboVtaBin!=null) 
				{
					
					System.out.println("cotixacion dentro de evento is bin "+ cotizacion.getCliente().getId());
					//Cliente clienteApa= cotizacion.getCliente();
					
					AparatoCliente apaCliBin = new AparatoCliente();
					
					/*if(comboVta==null)
					{
						Cliente cli= (Cliente) getEntityManager().createQuery("Select c from Cliente c where c.id=:idCliente").setParameter("idCliente",cotizacion.getCliente().getId()).getSingleResult();
						apaCliBin.setCliente(cli);
					}
					else*/
						apaCliBin.setCliente(cotizacion.getCliente());
					
					apaCliBin.setCostoVenta(getTotalCostosBin());
					apaCliBin.setCustomApa(false);
					apaCliBin.setEstado("PNP");
					if (tieneGarantiaBin)
						apaCliBin.setPeriodoGarantia(comboVtaBin
								.getPeriodoGarantia());
					
					apaCliBin.setLadoAparato(apaCli.getLadoAparatoBin());
					//apaCliBin.setCliente(apaCli.getCliente());
					apaCliBin.setDetalleAparato(apaCli.getDetalleAparato());
					apaCliBin.setRetroAuricular(apaCli.isRetroAuricular());
					apaCliBin.setFechaAdquisicion(new Date());
					apaCliBin.setHechoMedida(false);
					apaCliBin.setNombre(comboVtaBin.getNombre());
					
					//System.out.println("Cliente dentro de aparato cliente "+ apaCliBin.getCliente().getId());
					
					
					System.out.println("***dps Cliente actual cotizacion "+cotizacion.getCliente().getId());
					//System.out.println("***Cliente de instancia "+instance.getCliente());
					System.out.println("*** dps Cotizacion id "+ cotizacion.getId());
					System.out.println("**dps cotixacion dentro de evento is bin "+ cotizacion.getCliente().getId());
					
					//Verificamos si el aparato requiere trabajo de taller; elaboracion de molde o ensamble
					
						////////// Nuevo codigo para agregar orden de laboratorio por cada aparato si asi lo requieren
								for (ItemComboApa itemCmb : itemsComboApaBin)
								{
								
									if(itemCmb.isPrincipal())
									{
										apaCliBin.setModelo(itemCmb.getProducto().getModelo());
										apaCliBin.setMarca(itemCmb.getProducto().getMarca().getNombre());
										apaCliBin.setIdPrd(itemCmb.getProducto().getId());
										
										
										/*Item item = new Item();
										item.setCantidad(1);
										item.setInventario(itemCmb.getInventario());
										item.setItemId(new ItemId());
										item.getItemId().setInventarioId(
										itemCmb.getInventario().getId());
										item.setCodProducto(itemCmb.getCodProducto());
										item.setTipoPrecio(itemCmb.getTipPreCotizado());
										
										aparatoClienteHome.select(apaCliBin);
										aparatoClienteHome.getItems().clear();
										aparatoClienteHome.getItems().add(item);
										
										//aparatoClienteHome.getItems().addAll(item);
										aparatoClienteHome.save();*/
										
										System.out.println("Entro a es pricipal 2");
											if(itemCmb.getProducto().getCategoria().getReqMolde()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqMolde()==true)
											{
												
												getEntityManager().persist(apaCliBin);
												
												System.out.println("entro a molde 2");
												ReparacionCliente repCli = new ReparacionCliente();
												repCli.setAprobada(true);
												repCli.setAparatoRep(apaCliBin);
												repCli.setCliente(apaCliBin.getCliente());
												repCli.setDescripcion("Elaboracion de molde apartir de venta de aparato");
												repCli.setTipoRep("CRE");
												
												//Obteniendo sucursal
												if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
													repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
												else
													repCli.setSucursal(loginUser.getUser().getSucursal());
												
												
												repCli.setFechaIngreso(new Date());
												repCli.setEstado("PEN");
												//repCli.setIdCot(getCotizId()); //Este seria el id de la cotizacion binaural
												//repCli.setIdCot(itemsComboApaBin.get(0).getItemsCotizados().get(0).getCtCmbs().getCotizacion().getId());
												//repCli.setIdCot(cotizacion.getCotizacionComboBin().getId());
												//ExtraerIdbinaural
												/*CotizacionComboApa cotiBin=(CotizacionComboApa) getEntityManager().createQuery("SELECT coti FROM CorizacionComboApa coti where coti.cotizacionComboBin.id=:idCotizacion")
														.setParameter("idCotizacion", getCotizId()).getSingleResult();
												repCli.setIdCot(cotiBin.getId());*/
												repCli.setIdCot(getCotizId()+1);
												
												ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Fabricacion de molde'").getSingleResult();
												
												repCli.setProceso(procesoTall);
												
												getEntityManager().persist(repCli);
												
												//Luego de guardar la reparacion, generamos todas las etapas
												List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
															"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
															.setParameter("proceso", procesoTall).getResultList();
												
												EtapaRepCliente etRepCli = null; 
												Calendar fechaEst = new GregorianCalendar();
												fechaEst.setTime(repCli.getFechaIngreso());
												boolean isFirst = true;
												for(EtapaReparacion etapaRep: lstEtapas){
													etRepCli = new EtapaRepCliente();
													if(isFirst) { //Seteamos la primera 
														isFirst = false;
														etRepCli.setEstado("PEN");
														Calendar cal = new GregorianCalendar();
													//	cal.add(Calendar.DATE, 1);
														etRepCli.setFechaInicio(cal.getTime());
													}
													etRepCli.setEtapaRep(etapaRep);
													etRepCli.setReparacionCli(repCli);
													etRepCli.setUsuario(loginUser.getUser());
													//Calculamos la fecha estimada de finalizacion de cada etapa
													fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
													etRepCli.setFechaEstFin(fechaEst.getTime());
													etRepCli.setReparacionCli(repCli);
													getEntityManager().persist(etRepCli);
												    getEntityManager().flush();
												}
												
											}
											
											
											
											//Si requiere ensamblaje se realiza la orden
											if(itemCmb.getProducto().getCategoria().getReqEnsamble()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqEnsamble()==true)
											{
												
												getEntityManager().persist(apaCliBin);
												
												System.out.println("Entro a ensamblaje 2");
												ReparacionCliente repCli = new ReparacionCliente();
												repCli.setAprobada(true);
												repCli.setAparatoRep(apaCliBin);
												repCli.setCliente(apaCliBin.getCliente());
												repCli.setDescripcion("Orden de ensamblaje de aparato apartir de venta de aparato");
												repCli.setTipoRep("CRE");
												
												//Obteniendo sucursal
												if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
													repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
												else
													repCli.setSucursal(loginUser.getUser().getSucursal());
												
												
												repCli.setFechaIngreso(new Date());
												repCli.setEstado("PEN");
												//repCli.setIdCot(getCotizId());
												//repCli.setIdCot(itemsComboApaBin.get(0).getItemsCotizados().get(0).getCtCmbs().getCotizacion().getId());
												//repCli.setIdCot(cotizacion.getCotizacionComboBin().getId());
												
												//ExtraerIdbinaural
												/*CotizacionComboApa cotiBin=(CotizacionComboApa) getEntityManager().createQuery("SELECT coti FROM CorizacionComboApa coti where coti.cotizacionComboBin.id=:idCotizacion")
														.setParameter("idCotizacion", getCotizId()).getSingleResult();
												repCli.setIdCot(cotiBin.getId());*/
												repCli.setIdCot(getCotizId()+1);
																
												
												ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Ensamblaje de aparato'").getSingleResult();
												
												repCli.setProceso(procesoTall);
												
												getEntityManager().persist(repCli);
												
												//Luego de guardar la reparacion, generamos todas las etapas
												List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
															"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
															.setParameter("proceso", procesoTall).getResultList();
												
												EtapaRepCliente etRepCli = null; 
												Calendar fechaEst = new GregorianCalendar();
												fechaEst.setTime(repCli.getFechaIngreso());
												boolean isFirst = true;
												for(EtapaReparacion etapaRep: lstEtapas){
													etRepCli = new EtapaRepCliente();
													if(isFirst) { //Seteamos la primera 
														isFirst = false;
														etRepCli.setEstado("PEN");
														Calendar cal = new GregorianCalendar();
													//	cal.add(Calendar.DATE, 1);
														etRepCli.setFechaInicio(cal.getTime());
													}
													etRepCli.setEtapaRep(etapaRep);
													etRepCli.setReparacionCli(repCli);
													etRepCli.setUsuario(loginUser.getUser());
													//Calculamos la fecha estimada de finalizacion de cada etapa
													fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
													etRepCli.setFechaEstFin(fechaEst.getTime());
													etRepCli.setReparacionCli(repCli);
													getEntityManager().persist(etRepCli);
												    getEntityManager().flush();
												}
												
											}
											
										
										
										//////////////////
									}
							}
					
					/*CotizacionComboApa cotiBin=(CotizacionComboApa) getEntityManager().createQuery("Select c From CotizacionComboApa c where c.id=:idCotiBin")
							.setParameter("idCotiBin", getCotizId()+1).getSingleResult();
					
					cotiBin.setCuentaCobrar(cxc);
					getEntityManager().merge(cotiBin);*/
					
				}
		
		
		
		
		try {
			System.out
					.println("\n\n\n\n\n\nEntrando a persistir nuevamente la cotizacion");
			getEntityManager().merge(cotizacion);
			getEntityManager().flush();
			System.out.println("Terminamos de persistir");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Se agrego una cuenta por cobrar y se bloqueo la venta");

	}

	public Float obtenerPrecioCotizado(ItemComboApa item) {
		System.out.println("Entr� a obtenerprecioCotizado");
		for (CotCmbsItems tmpItm : itemsCotizados) {
			if (tmpItm.getItem().getId().equals(item.getId())) {
				System.out.println("Retornando precio cotizado"
						+ tmpItm.getPrecioCotizado());
				return tmpItm.getPrecioCotizado();
			}
		}
		return null;
	}

	public String obtenerTipPreCotizado(ItemComboApa item) {
		for (CotCmbsItems tmpItm : itemsCotizados) {
			if (tmpItm.getItem().equals(item)) {
				return tmpItm.getTipoPrecio();
			}
		}
		return null;
	}
	
	// M�todo que recorre la lista de combos cotizados y actualiza los precios para cada item.
	public void actualizarPreCotizados() {
		
		List<ItemComboApa> itemsCotizados = new ArrayList<ItemComboApa>();
		List<ComboAparato> combosCotizados = new ArrayList<ComboAparato>();
		if (isBinaural()) {
			for (ComboAparato tmpCmb : selCmbsListBin) {
				for (ItemComboApa tmpItm : tmpCmb.getItemsCombo()) {

					if (tmpItm.getProducto() != null) {
						if (tmpItm.getTipoPrecio().equals("NRM")) {
							tmpItm.setPrecioCotizado(tmpItm.getProducto()
									.getPrcNormal());
							tmpItm.setTipPreCotizado("NRM");
						} else if (tmpItm.getTipoPrecio().equals("MIN")) {
							tmpItm.setPrecioCotizado(tmpItm.getProducto()
									.getPrcMinimo());
							tmpItm.setTipPreCotizado("MIN");
						} else if (tmpItm.getTipoPrecio().equals("OFE")) {
							tmpItm.setPrecioCotizado(tmpItm.getProducto()
									.getPrcOferta());
							tmpItm.setTipPreCotizado("OFE");
						}
						itemsCotizados.add(tmpItm);
					}
				}
				tmpCmb.setItemsCombo(itemsCotizados);
				itemsCotizados = new ArrayList<ItemComboApa>();
				combosCotizados.add(tmpCmb);
			}
			selCmbsListBin = combosCotizados;
			combosCotizados = new ArrayList<ComboAparato>();
		}

		for (ComboAparato tmpCmb : selCmbsList) {
			for (ItemComboApa tmpItm : tmpCmb.getItemsCombo()) {

				if (tmpItm.getProducto() != null) {
					if (tmpItm.getTipoPrecio().equals("NRM")) {
						tmpItm.setPrecioCotizado(tmpItm.getProducto()
								.getPrcNormal());
						tmpItm.setTipPreCotizado("NRM");
					} else if (tmpItm.getTipoPrecio().equals("MIN")) {
						tmpItm.setPrecioCotizado(tmpItm.getProducto()
								.getPrcMinimo());
						tmpItm.setTipPreCotizado("MIN");
					} else if (tmpItm.getTipoPrecio().equals("OFE")) {
						tmpItm.setPrecioCotizado(tmpItm.getProducto()
								.getPrcOferta());
						tmpItm.setTipPreCotizado("OFE");
					}
					itemsCotizados.add(tmpItm);
				}
			}
			tmpCmb.setItemsCombo(itemsCotizados);
			itemsCotizados = new ArrayList<ItemComboApa>();
			combosCotizados.add(tmpCmb);
		}
		selCmbsList = combosCotizados;
		combosCotizados = new ArrayList<ComboAparato>();
	}

	public void recalcularTotalVenta() {
		System.out.println("SELBINAURAL=" +getSelBinaural());
		if (cotizacion!=null){
		actualizarPreCotizados();}
		// Sacamos el total de los items
		Float totalItems = 0f, totalFinal = 0f;
		List<ItemComboApa> tmpItmLst = new ArrayList<ItemComboApa>();
		if (cotizacion!= null && cotizacion.getEstado().equals("COT")){
		if (selBinaural == 1)
			tmpItmLst = itemsComboApa;
		else
			tmpItmLst = itemsComboApaBin;
		}
		else if (cotizacion!=null && cotizacion.getEstado().equals("PEN")){
			if (selBinaural == 1 && comboVta!=null)
				tmpItmLst = comboVta.getItemsCombo();
			else if (selBinaural == 2 && comboVtaBin!=null)
				tmpItmLst = comboVtaBin.getItemsCombo();
		} 
		else return;
	
		for (ItemComboApa tmpItm : tmpItmLst)
			if (tmpItm.getProducto() != null) {
				totalItems += tmpItm.getPrecioCotizado();
			}

		// En base al total de los costos, sacamos el detalle de costos
		totalFinal += totalItems;
		if (selBinaural == 1 && comboVta != null) {
			for (CostoServicio tmpCst : comboVta.getCostosCombo()) {
				totalFinal += tmpCst.getServicio().getCosto().floatValue();
				System.out
						.println(tmpCst.getServicio().getCosto().floatValue());
			}
			setTotalCostos(totalFinal);
			setTotalItems(totalItems);
		} else {
			if (selBinaural==2 && comboVtaBin != null)
				for (CostoServicio tmpCst : comboVtaBin.getCostosCombo())
					totalFinal += tmpCst.getServicio().getCosto().floatValue();

			setTotalCostosBin(totalFinal);
			setTotalItemsBin(totalItems);
		}

		totalMagnanime = 0f;
		System.out.println("TotalCostos: " + totalCostos);
		System.out.println("TotalCostosBin: " + totalCostosBin);
		if (totalCostos != null)
			setTotalMagnanime(totalCostos);
		if (totalCostosBin != null)
			setTotalMagnanime(getTotalMagnanime() + totalCostosBin);
		if (ventaItemHome != null && ventaItemHome.getSubTotal() != null)
			setTotalMagnanime(getTotalMagnanime() + ventaItemHome.getSubTotal());
		if (ventaItemHome != null && ventaItemHome.getSubTotalSrv() != null)
			setTotalMagnanime(getTotalMagnanime()
					+ ventaItemHome.getSubTotalSrv());
	}

	@Override
	public boolean preModify() {
		if (instance.getSucursal() == null) {
			FacesMessages.instance().add(Severity.ERROR,
					sainv_messages.get("vtaitm_error_save1"));
			return false;
		}

		return true;
	}

	@Override
	public void posSave() {
		try {
			// Reducimos las existencias del inventario y los numeros de serie o
			// de
			// lote
			// Generamos el movimiento del inventario
			Movimiento mov = new Movimiento();
			mov.setFecha(new Date());
			mov.setSucursal(loginUser.getUser().getSucursal());
			mov.setTipoMovimiento("S");
			mov.setUsuario(loginUser.getUser());
			mov.setRazon("V");
			mov.setObservacion("Salida por la venta generada automaticamente de un combo de aparato, detalle de la venta:"
					+ instance.getDetalle());

			
			// Generamos el aparato del cliente
			AparatoCliente apaCli = aparatoClienteHome.getInstance();
			
			
			Map<Integer, ItemComboApa> lstProdsMov = new HashMap<Integer, ItemComboApa>(20);
			Map<Integer, Item> lstProdsAdic = new HashMap<Integer, Item>(20);
			Map<Integer, ItemComboApa> lstProdsMovBin = new HashMap<Integer, ItemComboApa>(20);   //Este codigo se cambio de posicion;
			List<Item> itemsBin = new ArrayList<Item>();
			//System.out.println("******"+instance.getCliente().getFullName());
			if(comboVta!=null)
			{

				apaCli.setCliente(instance.getCliente());
				apaCli.setActivo(true);
				apaCli.setCostoVenta(getTotalCostos());
				apaCli.setCustomApa(false);
				apaCli.setEstado("ACT");
				if (tieneGarantia)
					apaCli.setPeriodoGarantia(comboVta.getPeriodoGarantia());
				apaCli.setFechaAdquisicion(new Date());
				apaCli.setHechoMedida(false);
				apaCli.setNombre(comboVta.getNombre());
				
				/*Map<Integer, ItemComboApa> lstProdsMov = new HashMap<Integer, ItemComboApa>(
						20);
				Map<Integer, Item> lstProdsAdic = new HashMap<Integer, Item>(20);
				Map<Integer, ItemComboApa> lstProdsMovBin = new HashMap<Integer, ItemComboApa>(   //Este codigo se cambio de posicion
						20);
				List<Item> itemsBin = new ArrayList<Item>();*/
	
				
			
				for (ItemComboApa itemCmb : itemsComboApa) {
					//itemCmb.setId(null);
					//itemCmb.setCombo(null);
					if (!lstProdsMov.containsKey(itemCmb.getProducto().getId())) {
						
						
						System.out.println("*********************************************************************Precio de itemComboApa "+itemCmb.getPrecioCotizado());
						//Poner aqi el precioCotizado al valor, costo unitario
						
						itemCmb.setPrecioCotizado(itemCmb.getPrecioCotizado());
						
						if(itemCmb.getCodProducto()!=null)
							itemCmb.setCodProducto(itemCmb.getCodProducto());
						
						lstProdsMov.put(itemCmb.getProducto().getId(), itemCmb);
						itemCmb.setCantidad((short) 1);
					} else {
						System.out.println("Precio de itemComboApa "+itemCmb.getPrecioCotizado());
						short qty = lstProdsMov.get(itemCmb.getProducto().getId()).getCantidad();
						lstProdsMov.get(itemCmb.getProducto().getId()).setCantidad(
								++qty);
					}
	
					Item item = new Item();
					item.setCantidad(1);
					item.setInventario(itemCmb.getInventario());
					item.setItemId(new ItemId());
					item.getItemId().setInventarioId(
					itemCmb.getInventario().getId());
					//item.setCodProducto(itemCmb.getCodProducto());
					//item.setCodProducto(codCombo1);
					item.setTipoPrecio(itemCmb.getTipPreCotizado());
					
	
					if (itemCmb.isPrincipal())
					{
						item.setCodProducto(codCombo1);
						item.setPrincipal(true);
						
					}
					
					
					// Guardamos los codigos si es que tienen codigos
					if (itemCmb.getProducto().getCategoria().isTieneNumSerie() || itemCmb.getProducto().getCategoria().isTieneNumLote()) {
						/*itemCmb.getCodProducto().setEstado("USD");
						getEntityManager().merge(itemCmb.getCodProducto());*/
						codCombo1.setEstado("USD");
						getEntityManager().merge(codCombo1);
					}
					
					
					aparatoClienteHome.getItems().add(item);
					
					// Generamos el detalle de la venta
					DetVentaProdServ detVta = new DetVentaProdServ();
					detVta.setCantidad(item.getCantidad());
					detVta.setMonto(itemCmb.getPrecioCotizado());
					detVta.setVenta(instance);
					detVta.setCodExacto(item.getInventario().getProducto()
							.getReferencia());
					detVta.setCodClasifVta("PRD");
					detVta.setCosto(item.getInventario().getProducto().getCosto());
	
					if (itemCmb.getCodProducto() != null
							&& itemCmb.getCodProducto().getNumSerie() != null)
						detVta.setNumSerie(itemCmb.getCodProducto().getNumSerie());
					if (itemCmb.getCodProducto() != null
							&& itemCmb.getCodProducto().getNumLote() != null)
						detVta.setNumLote(itemCmb.getCodProducto().getNumLote());
	
					// Formamos el detalle
					StringBuilder bld = new StringBuilder();
					bld.append(item.getInventario().getProducto().getNombre());
					if (item.getInventario().getProducto().getModelo() != null)
						bld.append(", Modelo "
								+ item.getInventario().getProducto().getModelo());
					if (item.getInventario().getProducto().getMarca() != null)
						bld.append(", Marca "
								+ item.getInventario().getProducto().getMarca()
										.getNombre());
					detVta.setDetalle(bld.toString());
					
					//nuevo 09/11/2016
					detVta.setCombo(comboVta);
					
					//nuevo 24/11/2016
					detVta.setCodCoti(cotizacion.getId());
					
					if(itemCmb.isPrincipal())
					{
						System.out.println("*** asignar serie");
						if(itemCmb.getCategoria().isTieneNumLote())
							detVta.setNumLote(codCombo1.getNumLote());
						
						if(itemCmb.getCategoria().isTieneNumSerie())
							detVta.setNumSerie(codCombo1.getNumSerie());
					}
					
					detVta.setProducto(itemCmb.getProducto());//nuevo agregado 09/11/2016
					getEntityManager().persist(detVta);//Descomentado 29/11/2016
					
					
					
					
				}
			}

			AparatoCliente apaCliBin = new AparatoCliente();
			if (isBinaural() && comboVtaBin!=null) {
				//System.out.println("cliente instancia: "+ instance.getCliente().getFullName());
				apaCliBin.setCliente(instance.getCliente());
				
				apaCliBin.setCostoVenta(getTotalCostosBin());
				apaCliBin.setCustomApa(false);
				apaCliBin.setEstado("ACT");
				if (tieneGarantiaBin)
					apaCliBin.setPeriodoGarantia(comboVtaBin
							.getPeriodoGarantia());
				apaCliBin.setLadoAparato(apaCli.getLadoAparatoBin());
				apaCliBin.setCliente(apaCli.getCliente());
				apaCliBin.setDetalleAparato(apaCli.getDetalleAparato());
				apaCliBin.setRetroAuricular(apaCli.isRetroAuricular());
				apaCliBin.setFechaAdquisicion(new Date());
				apaCliBin.setHechoMedida(false);
				apaCliBin.setNombre(comboVtaBin.getNombre());

				
					/*Map<Integer, ItemComboApa> lstProdsMovBin = new HashMap<Integer, ItemComboApa>(
							20);
					List<Item> itemsBin = new ArrayList<Item>();*/
				
				//itemsComboApaBin
				for (ItemComboApa itemCmbBin : itemsComboApaBin) {
					
					System.out.println("*** Entro al for de itemsComboApaBin ");
					//itemCmbBin.setId(null);
					//itemCmbBin.setCombo(null);
					
					if (!lstProdsMovBin.containsKey(itemCmbBin.getProducto()
							.getId())) {
						
						System.out.println("Precio de itemComboApa "+itemCmbBin.getPrecioCotizado());
						itemCmbBin.setPrecioCotizado(itemCmbBin.getPrecioCotizado());
						
						lstProdsMovBin.put(itemCmbBin.getProducto().getId(),
								itemCmbBin);
						itemCmbBin.setCantidad((short) 1);
					} else {
						System.out.println("Precio de itemComboApa "+itemCmbBin.getPrecioCotizado());
						short qty = lstProdsMovBin.get(
								itemCmbBin.getProducto().getId()).getCantidad();
						lstProdsMovBin.get(itemCmbBin.getProducto().getId())
								.setCantidad(++qty);
					}

					Item item = new Item();
					item.setCantidad(1);
					item.setInventario(itemCmbBin.getInventario());
					item.setItemId(new ItemId());
					item.getItemId().setInventarioId(
							itemCmbBin.getInventario().getId());
					//item.setCodProducto(itemCmbBin.getCodProducto());
					//item.setCodProducto(codCombo2);
					item.setTipoPrecio(itemCmbBin.getTipPreCotizado());


					if (itemCmbBin.isPrincipal())
					{
						item.setCodProducto(codCombo2);
						item.setPrincipal(true);
					}
					
					// Guardamos los codigos si es que tienen codigos
					if (itemCmbBin.getProducto().getCategoria()
							.isTieneNumSerie()
							|| itemCmbBin.getProducto().getCategoria()
									.isTieneNumLote()) {
						
						/*itemCmbBin.getCodProducto().setEstado("USD");
						getEntityManager().merge(itemCmbBin.getCodProducto());*/
						
						codCombo2.setEstado("USD");
						getEntityManager().merge(codCombo2);
					}
					
					
					itemsBin.add(item);

					// Generamos el detalle de la venta
					DetVentaProdServ detVta = new DetVentaProdServ();
					detVta.setCantidad(item.getCantidad());
					detVta.setMonto(itemCmbBin.getPrecioCotizado());
					detVta.setVenta(instance);
					detVta.setCodExacto(item.getInventario().getProducto()
							.getReferencia());
					detVta.setCodClasifVta("PRD");
					detVta.setCosto(item.getInventario().getProducto()
							.getCosto());

					if (itemCmbBin.getCodProducto() != null
							&& itemCmbBin.getCodProducto().getNumSerie() != null)
						detVta.setNumSerie(itemCmbBin.getCodProducto()
								.getNumSerie());
					if (itemCmbBin.getCodProducto() != null
							&& itemCmbBin.getCodProducto().getNumLote() != null)
						detVta.setNumLote(itemCmbBin.getCodProducto()
								.getNumLote());

					// Formamos el detalle
					StringBuilder bld = new StringBuilder();
					bld.append(item.getInventario().getProducto().getNombre());
					if (item.getInventario().getProducto().getModelo() != null)
						bld.append(", Modelo "
								+ item.getInventario().getProducto()
										.getModelo());
					if (item.getInventario().getProducto().getMarca() != null)
						bld.append(", Marca "
								+ item.getInventario().getProducto().getMarca()
										.getNombre());
					detVta.setDetalle(bld.toString());
					
					detVta.setProducto(itemCmbBin.getProducto()); //Nuevo 2016-11-09
					detVta.setCombo(comboVtaBin);
					detVta.setCodCoti(cotizacion.getHijoBin().get(0).getId());
					
					
					if(itemCmbBin.isPrincipal())
					{
						System.out.println("*** asignar serie");
						if(itemCmbBin.getCategoria().isTieneNumLote())
							detVta.setNumLote(codCombo2.getNumLote());
						
						if(itemCmbBin.getCategoria().isTieneNumSerie())
							detVta.setNumSerie(codCombo2.getNumSerie());
					}
					
					
					getEntityManager().persist(detVta);  //Para crear detalle binaboral //Descomentado el 29/11/2016
				}
			}
			
			
			if(comboVta!=null)
			{
				// Servicios del combo
				    for (CostoServicio srv : comboVta.getCostosCombo()) {
				    	
				    totalservprod+=srv.getServicio().getCosto().floatValue();
					DetVentaProdServ dtVta = new DetVentaProdServ();
					dtVta.setCantidad(1);
					StringBuilder bld = new StringBuilder();
					bld.append(srv.getServicio().getName());
					dtVta.setDetalle(bld.toString());
					dtVta.setEscondido(true);
					dtVta.setMonto(srv.getServicio().getCosto().floatValue());
					dtVta.setVenta(instance);
					dtVta.setCodClasifVta("SRV");
					dtVta.setCodExacto(srv.getServicio().getCodigo());
					dtVta.setServicio(srv.getServicio());
					getEntityManager().persist(dtVta); //Descomentado el 29/11/2016
				}
			}

			// Productos adicionales al combo agregados
			 for (Item item : ventaItemHome.getItemsAgregados()) {
				item.getItemId().setMovimientoId(instance.getId());

				if (!lstProdsAdic.containsKey(item.getInventario()
						.getProducto().getId())) {
					lstProdsAdic.put(
							item.getInventario().getProducto().getId(), item);

				} else {
					int qty = lstProdsAdic.get(
							item.getInventario().getProducto().getId())
							.getCantidad();
					lstProdsAdic
							.get(item.getInventario().getProducto().getId())
							.setCantidad(++qty);
				}
				/*
				 * item.setMovimiento(mov); itemHome.setInstance(item);
				 * itemHome.save();
				 */
				// Guardamos los codigos si es que tienen codigos
			
				if (item.getInventario().getProducto().getCategoria()
						.isTieneNumSerie()
						|| item.getInventario().getProducto().getCategoria()
								.isTieneNumLote()) {
					ArrayList<CodProducto> codsProds = ventaItemHome
							.getLstCodsProductos().get(
									item.getInventario().getProducto()
											.getReferencia());
					for (CodProducto tmpCd : codsProds) {
						if (tmpCd.isTransferido()) {
							tmpCd.setEstado("USD");
							getEntityManager().merge(tmpCd);
						}
					}
				}

				// Generamos el detalle de la venta
				DetVentaProdServ detVta = new DetVentaProdServ();
				detVta.setCantidad(item.getCantidad());
				if (item.getTipoPrecio().equals("NRM")
						&& item.getInventario().getProducto().getPrcNormal() != null)
					detVta.setMonto(item.getInventario().getProducto()
							.getPrcNormal());
				else if (item.getTipoPrecio().equals("MIN")
						&& item.getInventario().getProducto().getPrcMinimo() != null)
					detVta.setMonto(item.getInventario().getProducto()
							.getPrcMinimo());
				else if (item.getTipoPrecio().equals("OFE")
						&& item.getInventario().getProducto().getPrcOferta() != null)
					detVta.setMonto(item.getInventario().getProducto()
							.getPrcOferta());
				detVta.setVenta(instance);
				detVta.setCodExacto(item.getInventario().getProducto()
						.getReferencia());
				detVta.setCodClasifVta("PRD");
				detVta.setCosto(item.getInventario().getProducto().getCosto());
				totalservprod+=item.getInventario().getProducto().getCosto();

				String numsSeries = "", numsLotes = "";
				ArrayList<CodProducto> codsProds = ventaItemHome
						.getLstCodsProductos().get(
								item.getInventario().getProducto()
										.getReferencia());
				if (codsProds != null && codsProds.size() > 0)
					for (CodProducto tmpCd : codsProds) {
						if (tmpCd.isTransferido()
								&& tmpCd.getNumSerie() != null
								&& !tmpCd.getNumSerie().equals(""))
							numsSeries = numsSeries.concat(tmpCd.getNumSerie()
									.trim() + ",");
						if (tmpCd.isTransferido() && tmpCd.getNumLote() != null
								&& !tmpCd.getNumLote().equals(""))
							numsLotes = numsLotes.concat(tmpCd.getNumLote()
									.trim() + ",");
					}
				detVta.setNumSerie(detVta.getNumSerie().concat(numsSeries));
				detVta.setNumLote(detVta.getNumLote().concat(numsLotes));

				// Formamos el detalle
				StringBuilder bld = new StringBuilder();
				bld.append(item.getInventario().getProducto().getNombre());
				if (item.getInventario().getProducto().getModelo() != null)
					bld.append(", Modelo "
							+ item.getInventario().getProducto().getModelo());
				if (item.getInventario().getProducto().getMarca() != null)
					bld.append(", Marca "
							+ item.getInventario().getProducto().getMarca()
									.getNombre());
				if (detVta.getNumSerie() != null
						&& !detVta.getNumSerie().equals(""))
					bld.append(", Series " + detVta.getNumSerie());
				if (detVta.getNumLote() != null
						&& !detVta.getNumLote().equals(""))
					bld.append(", Lote " + detVta.getNumLote());
				detVta.setDetalle(bld.toString());
				//getEntityManager().persist(detVta);
			} 

			// Servicios adicionales al combo agregados
			for (Service srv : ventaItemHome.getServiciosAgregados()) {

				totalservprod+=srv.getCosto().floatValue();
				// Generamos el detalle de la venta
				DetVentaProdServ detVta = new DetVentaProdServ();
				detVta.setCantidad(srv.getCantidad());
				detVta.setMonto(srv.getCosto().floatValue());
				detVta.setVenta(instance);
				detVta.setServicio(srv);
				detVta.setCodExacto(srv.getCodigo());
				detVta.setCodClasifVta("SRV");
				// Formamos el detalle
				StringBuilder bld = new StringBuilder();
				bld.append(srv.getName());

				detVta.setDetalle(bld.toString());
				//getEntityManager().persist(detVta);
			}
			
			
			/*if(comboVta!=null)
			{
			
				DetVentaProdServ detVta = new DetVentaProdServ();
				detVta.setCantidad(1);
				//detVta.setMonto( +totalservprod);
				detVta.setMonto(totalCostos);
				totalservprod = 0f;
				// detVta.setMonto(0f);
				detVta.setVenta(instance);
				detVta.setCodExacto(comboVta.getCodigo());
				detVta.setCodClasifVta("CMB");
				// Formamos el detalle
				StringBuilder bld = new StringBuilder();
				bld.append("COMBO " + comboVta.getNombre());
	
				detVta.setDetalle(bld.toString());
			
				
				//nuevo 09/11/2016
				detVta.setCombo(comboVta);
				
				//nuevo 24/11/2016
				detVta.setCodCoti(cotizacion.getId());
			
				for(ItemComboApa itm:itemsComboApa)
				{
					if(itm.isPrincipal())
					{
						System.out.println("*** asignar serie");
						if(itm.getCategoria().isTieneNumLote())
							detVta.setNumLote(codCombo1.getNumLote());
						
						if(itm.getCategoria().isTieneNumSerie())
							detVta.setNumSerie(codCombo1.getNumSerie());
					}
					
				}
				
				getEntityManager().persist(detVta);
			
			}*/
			

			if (isBinaural() && comboVtaBin!=null) {
				
				System.out.println("*** Entro a servicios del combo binaural ");
				/*DetVentaProdServ detVtaBin = new DetVentaProdServ();
				detVtaBin.setCantidad(1);
				//detVtaBin.setMonto(totalCostosBin+totalItemsBin); //EditadoBinaural
				detVtaBin.setMonto(totalCostosBin);
				//System.out.println("totalCostosBin: "+ totalCostosBin + " totalItemsBin:  " + totalItemsBin + "totalservprod: "+ totalservprod + "totalCostos: " + totalCostos);
				// detVtaBin.setMonto(0f);
				detVtaBin.setVenta(instance);
				detVtaBin.setCodExacto(comboVtaBin.getCodigo());
				detVtaBin.setCodClasifVta("CMB");
				// Formamos el detalle
				StringBuilder bld = new StringBuilder();
				bld.append("COMBO " + comboVtaBin.getNombre());

				detVtaBin.setDetalle(bld.toString());
				detVtaBin.setCombo(comboVtaBin);
				
				detVtaBin.setCodCoti(cotizacion.getHijoBin().get(0).getId());
				
				for(ItemComboApa itm:itemsComboApaBin)
				{
					if(itm.isPrincipal())
					{
						if(itm.getCategoria().isTieneNumLote())
							detVtaBin.setNumLote(codCombo2.getNumLote());
						
						if(itm.getCategoria().isTieneNumSerie())
							detVtaBin.setNumSerie(codCombo2.getNumSerie());
					}
					
				}
				
				getEntityManager().persist(detVtaBin);*/

				// Servicios del combo
				 for (CostoServicio srv : comboVtaBin.getCostosCombo()) {
					DetVentaProdServ dtVta = new DetVentaProdServ();
					dtVta.setCantidad(1);
					StringBuilder bld = new StringBuilder();
					bld.append(srv.getServicio().getName());
					dtVta.setDetalle(bld.toString());
					dtVta.setEscondido(true);
					dtVta.setMonto(srv.getServicio().getCosto().floatValue());
					dtVta.setVenta(instance);
					dtVta.setCodExacto(srv.getServicio().getCodigo());
					dtVta.setCodClasifVta("SRV");
					dtVta.setServicio(srv.getServicio());
					getEntityManager().persist(dtVta);
				}
			}

			Set<Integer> prdsRep = new HashSet<Integer>();

			for (Integer idPrd : lstProdsMov.keySet()) {
				Item item = new Item();
				item.setCantidad(lstProdsMov.get(idPrd).getCantidad()
						.intValue());
				item.setInventario(lstProdsMov.get(idPrd).getInventario());
				item.setItemId(new ItemId());
				item.getItemId().setInventarioId(item.getInventario().getId());
				item.setCostoUnitario(lstProdsMov.get(idPrd).getProducto().getCosto());
				item.setPrecioVenta(lstProdsMov.get(idPrd).getPrecioCotizado());// Se cambio el 06/12/2016
				System.out.println("************************Item comb "+lstProdsMov.get(idPrd).getPrecioCotizado());
				System.out.println("*****************************Itemmmmmm "+item.getCostoUnitario());
				if(lstProdsMov.get(idPrd).getCodProducto()!=null)
					item.setCodsSerie(lstProdsMov.get(idPrd).getCodProducto().getNumSerie());
				
				//System.out.println();
				if (lstProdsMovBin.keySet().contains(idPrd)) {
					Integer cantAct = item.getCantidad();
					cantAct += lstProdsMovBin.get(idPrd).getCantidad();
					item.setCantidad(cantAct);
					prdsRep.add(idPrd);
				}
				movimientoHome.getItemsAgregados().add(item);
			}

			for (Integer idPrd : lstProdsMovBin.keySet()) {
				if (!prdsRep.contains(idPrd)) {
					Item item = new Item();
					item.setCantidad(lstProdsMovBin.get(idPrd).getCantidad()
							.intValue());
					item.setInventario(lstProdsMovBin.get(idPrd)
							.getInventario());
					item.setItemId(new ItemId());
					//item.setCostoUnitario(lstProdsMovBin.get(idPrd).getPrecioCotizado());// Se cambio el 06/12/2016
					item.setCostoUnitario(lstProdsMovBin.get(idPrd).getProducto().getCosto());
					item.setPrecioVenta(lstProdsMovBin.get(idPrd).getPrecioCotizado());
					item.getItemId().setInventarioId(
							item.getInventario().getId());
					movimientoHome.getItemsAgregados().add(item);
				}
			}

			for (Integer idPrd : lstProdsAdic.keySet()) {
				if (!prdsRep.contains(idPrd)) {
					Item item = lstProdsAdic.get(idPrd);

					item.getItemId().setInventarioId(
							item.getInventario().getId());
					movimientoHome.getItemsAgregados().add(item);
				}
			}
			
			if(comboVta!=null)
			{
				aparatoClienteHome.save();
				
				////////// Nuevo codigo para agregar orden de laboratorio por cada aparato si asi lo requieren
					for (ItemComboApa itemCmb : itemsComboApa)
					{
					
						if(itemCmb.isPrincipal())
						{
							
							System.out.println("Es principal 1 ");
								//Si requiere elaboracion de molde, se realiza la orden de laboratorio
								if(itemCmb.getProducto().getCategoria().getReqMolde()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqMolde()==true)
								{
									System.out.println("�ntro a molde 1");
									ReparacionCliente repCli = new ReparacionCliente();
									repCli.setAprobada(true);
									repCli.setAparatoRep(apaCli);
									repCli.setCliente(apaCli.getCliente());
									repCli.setDescripcion("Elaboracion de molde apartir de venta de aparato");
									
									//Obteniendo sucursal
									if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
										repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
									else
										repCli.setSucursal(loginUser.getUser().getSucursal());
									
									
									repCli.setFechaIngreso(new Date());
									repCli.setEstado("PEN");
									repCli.setTipoRep("VNT");
									
									ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Fabricacion de molde'").getSingleResult();
									
									repCli.setProceso(procesoTall);
									
									getEntityManager().persist(repCli);
									
									//Luego de guardar la reparacion, generamos todas las etapas
									List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
												"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
												.setParameter("proceso", procesoTall).getResultList();
									
									EtapaRepCliente etRepCli = null; 
									Calendar fechaEst = new GregorianCalendar();
									fechaEst.setTime(repCli.getFechaIngreso());
									boolean isFirst = true;
									for(EtapaReparacion etapaRep: lstEtapas){
										etRepCli = new EtapaRepCliente();
										if(isFirst) { //Seteamos la primera 
											isFirst = false;
											etRepCli.setEstado("PEN");
											Calendar cal = new GregorianCalendar();
										//	cal.add(Calendar.DATE, 1);
											etRepCli.setFechaInicio(cal.getTime());
										}
										etRepCli.setEtapaRep(etapaRep);
										etRepCli.setReparacionCli(repCli);
										etRepCli.setUsuario(loginUser.getUser());
										//Calculamos la fecha estimada de finalizacion de cada etapa
										fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
										etRepCli.setFechaEstFin(fechaEst.getTime());
										etRepCli.setReparacionCli(repCli);
										getEntityManager().persist(etRepCli);
									    getEntityManager().flush();
									}
									
								}
								
								//Si requiere ensamblaje se realiza la orden
								if(itemCmb.getProducto().getCategoria().getReqEnsamble()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqEnsamble()==true)
								{
									System.out.println("Entro a ensamblaje 1");
									ReparacionCliente repCli = new ReparacionCliente();
									repCli.setAprobada(true);
									repCli.setAparatoRep(apaCli);
									repCli.setCliente(apaCli.getCliente());
									repCli.setDescripcion("Orden de ensamblaje de aparato apartir de venta de aparato");
									
									//Obteniendo sucursal
									if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
										repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
									else
										repCli.setSucursal(loginUser.getUser().getSucursal());
									
									
									repCli.setFechaIngreso(new Date());
									repCli.setEstado("PEN");
									repCli.setTipoRep("VNT");
									
									ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Ensamblaje de aparato'").getSingleResult();
									
									repCli.setProceso(procesoTall);
									
									getEntityManager().persist(repCli);
									
									//Luego de guardar la reparacion, generamos todas las etapas
									List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
												"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
												.setParameter("proceso", procesoTall).getResultList();
									
									EtapaRepCliente etRepCli = null; 
									Calendar fechaEst = new GregorianCalendar();
									fechaEst.setTime(repCli.getFechaIngreso());
									boolean isFirst = true;
									for(EtapaReparacion etapaRep: lstEtapas){
										etRepCli = new EtapaRepCliente();
										if(isFirst) { //Seteamos la primera 
											isFirst = false;
											etRepCli.setEstado("PEN");
											Calendar cal = new GregorianCalendar();
										//	cal.add(Calendar.DATE, 1);
											etRepCli.setFechaInicio(cal.getTime());
										}
										etRepCli.setEtapaRep(etapaRep);
										etRepCli.setReparacionCli(repCli);
										etRepCli.setUsuario(loginUser.getUser());
										//Calculamos la fecha estimada de finalizacion de cada etapa
										fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
										etRepCli.setFechaEstFin(fechaEst.getTime());
										etRepCli.setReparacionCli(repCli);
										getEntityManager().persist(etRepCli);
									    getEntityManager().flush();
									}
									
								}
							
							
							
						}
				}
			} ////////////////////////////
			
			
			comboVta = null;
			lstProdsMov = null;

			movimientoHome.select(mov);
			movimientoHome.save();

			// Si se trata de una cotizacion la actualizamos con el id de la
			// venta y
			// cambiamos su estado
			if (cotizacion != null) {
				cotizacion.setEstado("APL");
				cotizacion.setIdVta(instance);
				cotizacion.setFechaVenta(new Date());
				getEntityManager().merge(cotizacion);
				if (isBinaural() && cotizacion.getHijoBin() != null
						&& cotizacion.getHijoBin().size() > 0) {
					CotizacionComboApa tmpCot = cotizacion.getHijoBin().get(0);
					tmpCot.setEstado("APL");
					tmpCot.setFechaVenta(new Date());
					tmpCot.setIdVta(instance);
					getEntityManager().merge(tmpCot);
				}
			}

			// Sacamos el aparato principal
			for (Item item : aparatoClienteHome.getItems()){
				System.out.println("Aparato cliente home item: "+item.getInventario().getProducto().getMarca());
				System.out.println("modelo: "+item.getInventario().getProducto().getModelo());
			}
			
			/*if(comboVta!=null)
			{
				aparatoClienteHome.save();
			}*/
				
			//System.out.println("penultima impresion cliente instancia ** "+ instance.getCliente().getNombreCompleto());
			//System.out.println("Nombre del cliente aparato binaural "+ apaCliBin.getCliente().getNombreCompleto());
			//System.out.println("contenido apabin "+ apaBin.getCliNombre());
			if(comboVta==null)
			{
				apaCliBin.setCliente(instance.getCliente());
			}
			if (isBinaural() && comboVtaBin!=null) {
				aparatoClienteHome.select(apaCliBin);
				aparatoClienteHome.getItems().clear();
				aparatoClienteHome.getItems().addAll(itemsBin);
				aparatoClienteHome.save();
				
				
				
				//Verificamos si el aparato requiere trabajo de taller; elaboracion de molde o ensamble
				
					////////// Nuevo codigo para agregar orden de laboratorio por cada aparato si asi lo requieren
							for (ItemComboApa itemCmb : itemsComboApaBin)
							{
							
								if(itemCmb.isPrincipal())
								{
									System.out.println("Entro a es pricipal 2");
										if(itemCmb.getProducto().getCategoria().getReqMolde()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqMolde()==true)
										{
											System.out.println("entro a molde 2");
											ReparacionCliente repCli = new ReparacionCliente();
											repCli.setAprobada(true);
											repCli.setAparatoRep(apaCliBin);
											repCli.setCliente(apaCliBin.getCliente());
											repCli.setDescripcion("Elaboracion de molde apartir de venta de aparato");
											
											//Obteniendo sucursal
											if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
												repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
											else
												repCli.setSucursal(loginUser.getUser().getSucursal());
											
											
											repCli.setFechaIngreso(new Date());
											repCli.setEstado("PEN");
											repCli.setTipoRep("VNT");
											
											ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Fabricacion de molde'").getSingleResult();
											
											repCli.setProceso(procesoTall);
											
											getEntityManager().persist(repCli);
											
											//Luego de guardar la reparacion, generamos todas las etapas
											List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
														"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
														.setParameter("proceso", procesoTall).getResultList();
											
											EtapaRepCliente etRepCli = null; 
											Calendar fechaEst = new GregorianCalendar();
											fechaEst.setTime(repCli.getFechaIngreso());
											boolean isFirst = true;
											for(EtapaReparacion etapaRep: lstEtapas){
												etRepCli = new EtapaRepCliente();
												if(isFirst) { //Seteamos la primera 
													isFirst = false;
													etRepCli.setEstado("PEN");
													Calendar cal = new GregorianCalendar();
												//	cal.add(Calendar.DATE, 1);
													etRepCli.setFechaInicio(cal.getTime());
												}
												etRepCli.setEtapaRep(etapaRep);
												etRepCli.setReparacionCli(repCli);
												etRepCli.setUsuario(loginUser.getUser());
												//Calculamos la fecha estimada de finalizacion de cada etapa
												fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
												etRepCli.setFechaEstFin(fechaEst.getTime());
												etRepCli.setReparacionCli(repCli);
												getEntityManager().persist(etRepCli);
											    getEntityManager().flush();
											}
											
										}
										
										
										
										//Si requiere ensamblaje se realiza la orden
										if(itemCmb.getProducto().getCategoria().getReqEnsamble()==true || itemCmb.getProducto().getCategoria().getCategoriaPadre().getReqEnsamble()==true)
										{
											System.out.println("Entro a ensamblaje 2");
											ReparacionCliente repCli = new ReparacionCliente();
											repCli.setAprobada(true);
											repCli.setAparatoRep(apaCliBin);
											repCli.setCliente(apaCliBin.getCliente());
											repCli.setDescripcion("Orden de ensamblaje de aparato apartir de venta de aparato");
											
											//Obteniendo sucursal
											if(loginUser.getUser().getSucursal().getSucursalSuperior()!=null || !loginUser.getUser().getSucursal().getSucursalSuperior().equals(""))
												repCli.setSucursal(loginUser.getUser().getSucursal().getSucursalSuperior());
											else
												repCli.setSucursal(loginUser.getUser().getSucursal());
											
											
											repCli.setFechaIngreso(new Date());
											repCli.setEstado("PEN");
											repCli.setTipoRep("VNT");
											
											ProcesoTaller procesoTall = (ProcesoTaller) getEntityManager().createQuery("SELECT p FROM ProcesoTaller p where p.nombre='Ensamblaje de aparato'").getSingleResult();
											
											repCli.setProceso(procesoTall);
											
											getEntityManager().persist(repCli);
											
											//Luego de guardar la reparacion, generamos todas las etapas
											List<EtapaReparacion> lstEtapas = getEntityManager().createQuery("SELECT e FROM EtapaReparacion e " +
														"	WHERE e.procesoTll = :proceso ORDER BY e.orden ASC")
														.setParameter("proceso", procesoTall).getResultList();
											
											EtapaRepCliente etRepCli = null; 
											Calendar fechaEst = new GregorianCalendar();
											fechaEst.setTime(repCli.getFechaIngreso());
											boolean isFirst = true;
											for(EtapaReparacion etapaRep: lstEtapas){
												etRepCli = new EtapaRepCliente();
												if(isFirst) { //Seteamos la primera 
													isFirst = false;
													etRepCli.setEstado("PEN");
													Calendar cal = new GregorianCalendar();
												//	cal.add(Calendar.DATE, 1);
													etRepCli.setFechaInicio(cal.getTime());
												}
												etRepCli.setEtapaRep(etapaRep);
												etRepCli.setReparacionCli(repCli);
												etRepCli.setUsuario(loginUser.getUser());
												//Calculamos la fecha estimada de finalizacion de cada etapa
												fechaEst.add(Calendar.DATE, etapaRep.getTiempoEstimado());
												etRepCli.setFechaEstFin(fechaEst.getTime());
												etRepCli.setReparacionCli(repCli);
												getEntityManager().persist(etRepCli);
											    getEntityManager().flush();
											}
											
										}
										
									
									
									//////////////////
								}
						}
				
				
				
			}
			
			
			
			
			
			
			

			getEntityManager().flush();
			getEntityManager().refresh(instance);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}

	}

	public Image getImgApaPrincipal(ComboAparato combo) {
		try {
			BufferedImage imag;
			for (ItemComboApa tmpItm : combo.getItemsCombo()) {
				if (tmpItm.isPrincipal()) {
					if (tmpItm.getProducto() != null
							&& tmpItm.getProducto().getImage() != null
							&& tmpItm.getProducto().getImage().length > 0) {
						System.out.println("Entr� a imagen del producto "
								+ tmpItm.getProducto().getNombre());
						imag = ImageIO.read(new ByteArrayInputStream(tmpItm
								.getProducto().getImage()));
						return imag;
					} else {
						System.out.println("Entre a No Image");
						ClassLoader classLoader = Thread.currentThread()
								.getContextClassLoader();
						System.out.println("Tengo el Class loader");
						String path = classLoader.getResource(
								"../../kubeImg/noimage.png").getPath();
						System.out.println(path);
						File pathToFile = new File(path);
						Image image = ImageIO.read(pathToFile);
						return image;
					}
				}
			}
		} catch (IOException e) {
			System.out.println("No se pudo convertir la imagen: "
					+ e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void posModify() {
		// TODO Auto-generated method stub
	}

	@Override
	public void posDelete() {
		// TODO Auto-generated method stub

	}

	public List<Sucursal> getSucursales() {
		return sucursales;
	}

	public void setSucursales(List<Sucursal> sucursales) {
		this.sucursales = sucursales;
	}

	public List<CodProducto> getCurrCodigos() {
		return currCodigos;
	}

	public void setCurrCodigos(List<CodProducto> currCodigos) {
		this.currCodigos = currCodigos;
	}

	@Override
	public boolean preDelete() {
		// TODO Auto-generated method stub
		return false;
	}

	public List<VentaProdServ> getResultList() {
		return resultList;
	}

	public void setResultList(List<VentaProdServ> resultList) {
		this.resultList = resultList;
	}

	public Float getSubTotal() {
		return subTotal;
	}

	public void setSubTotal(Float subTotal) {
		this.subTotal = subTotal;
	}

	public Integer getVtcId() {
		return vtcId;
	}

	public void setVtcId(Integer vtcId) {
		this.vtcId = vtcId;
	}

	public Sucursal getSucursalFlt() {
		return sucursalFlt;
	}

	public void setSucursalFlt(Sucursal sucursalFlt) {
		this.sucursalFlt = sucursalFlt;
	}

	public ComboAparato getComboVta() {
		return comboVta;
	}

	public void setComboVta(ComboAparato comboVta) {
		this.comboVta = comboVta;
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

	public ItemComboApa getSelItmCombo() {
		return selItmCombo;
	}

	public void setSelItmCombo(ItemComboApa selItmCombo) {
		this.selItmCombo = selItmCombo;
	}

	public List<CodProducto> getCodsProducto() {
		return codsProducto;
	}

	public void setCodsProducto(List<CodProducto> codsProducto) {
		this.codsProducto = codsProducto;
	}

	public List<ItemComboApa> getItemsComboApa() {
		return itemsComboApa;
	}

	public void setItemsComboApa(List<ItemComboApa> itemsComboApa) {
		this.itemsComboApa = itemsComboApa;
	}

	public List<CotizacionComboApa> getCotizacionList() {
		return cotizacionList;
	}

	public void setCotizacionList(List<CotizacionComboApa> cotizacionList) {
		this.cotizacionList = cotizacionList;
	}

	public Integer getCotizId() {
		return cotizId;
	}

	public void setCotizId(Integer cotizId) {
		this.cotizId = cotizId;
	}

	public CotizacionComboApa getCotizacion() {
		return cotizacion;
	}

	public void setCotizacion(CotizacionComboApa cotizacion) {
		this.cotizacion = cotizacion;
	}

	public boolean isBinaural() {
		return binaural;
	}

	public void setBinaural(boolean binaural) {
		this.binaural = binaural;
	}

	public Short getSelBinaural() {
		return selBinaural;
	}

	public void setSelBinaural(Short selBinaural) {
		this.selBinaural = selBinaural;
	}

	public ComboAparato getComboVtaBin() {
		return comboVtaBin;
	}

	public void setComboVtaBin(ComboAparato comboVtaBin) {
		this.comboVtaBin = comboVtaBin;
	}

	public Integer getSelectedComboVta() {
		return selectedComboVta;
	}

	public void setSelectedComboVta(Integer selectedComboVta) {
		this.selectedComboVta = selectedComboVta;
	}

	public Integer getSelectedComboVtaBin() {
		return selectedComboVtaBin;
	}

	public void setSelectedComboVtaBin(Integer selectedComboVtaBin) {
		this.selectedComboVtaBin = selectedComboVtaBin;
	}

	public List<ItemComboApa> getItemsComboApaBin() {
		return itemsComboApaBin;
	}

	public void setItemsComboApaBin(List<ItemComboApa> itemsComboApaBin) {
		this.itemsComboApaBin = itemsComboApaBin;
	}

	public Float getTotalCostosBin() {
		return totalCostosBin;
	}

	public void setTotalCostosBin(Float totalCostosBin) {
		this.totalCostosBin = totalCostosBin;
	}

	public Float getTotalItemsBin() {
		return totalItemsBin;
	}

	public void setTotalItemsBin(Float totalItemsBin) {
		this.totalItemsBin = totalItemsBin;
	}

	public boolean isTieneGarantia() {
		return tieneGarantia;
	}

	public void setTieneGarantia(boolean tieneGarantia) {
		this.tieneGarantia = tieneGarantia;
	}

	public boolean isTieneGarantiaBin() {
		return tieneGarantiaBin;
	}

	public void setTieneGarantiaBin(boolean tieneGarantiaBin) {
		this.tieneGarantiaBin = tieneGarantiaBin;
	}

	public Float getTotalMagnanime() {
		return totalMagnanime;
	}

	public void setTotalMagnanime(Float totalMagnanime) {
		this.totalMagnanime = totalMagnanime;
	}

	public String getNomCoinci() {
		return nomCoinci;
	}

	public void setNomCoinci(String nomCoinci) {
		this.nomCoinci = nomCoinci;
	}

	public List<ComboAparato> getSelCmbsList() {
		return selCmbsList;
	}

	public void setSelCmbsList(List<ComboAparato> selCmbsList) {
		this.selCmbsList = selCmbsList;
	}

	public CotizacionCombos getCotizacionCombos() {
		return cotizacionCombos;
	}

	public void setCotizacionCombos(CotizacionCombos cotizacionCombos) {
		this.cotizacionCombos = cotizacionCombos;
	}

	public List<ComboAparato> getSelCmbsListBin() {
		return selCmbsListBin;
	}

	public void setSelCmbsListBin(List<ComboAparato> selCmbsListBin) {
		this.selCmbsListBin = selCmbsListBin;
	}

	public boolean isShowTotal() {
		return showTotal;
	}

	public void setShowTotal(boolean showTotal) {
		this.showTotal = showTotal;
	}

	public List<CotCmbsItems> getItemsCotizados() {
		return itemsCotizados;
	}

	public void setItemsCotizados(List<CotCmbsItems> itemsCotizados) {
		this.itemsCotizados = itemsCotizados;
	}

	public List<CotCmbsItems> getItemsCotizadosBin() {
		return itemsCotizadosBin;
	}

	public void setItemsCotizadosBin(List<CotCmbsItems> itemsCotizadosBin) {
		this.itemsCotizadosBin = itemsCotizadosBin;
	}

	public int getValidez() {
		return validez;
	}

	public void setValidez(int validez) {
		this.validez = validez;
	}

	public Cliente getNombreCompleto() {
		return nombreCompleto;
	}

	public void setNombreCompleto(Cliente nombreCompleto) {
		this.nombreCompleto = nombreCompleto;
	}
	public List<CotizacionPrdSvcAdicionales> getPrdSvcAdicionales() {
		return prdSvcAdicionales;
	}

	public void setPrdSvcAdicionales(
			List<CotizacionPrdSvcAdicionales> prdSvcAdicionales) {
		this.prdSvcAdicionales = prdSvcAdicionales;
	}

	public CodProducto getCodCombo1() {
		return codCombo1;
	}

	public void setCodCombo1(CodProducto codCombo1) {
		this.codCombo1 = codCombo1;
	}

	public CodProducto getCodCombo2() {
		return codCombo2;
	}

	public void setCodCombo2(CodProducto codCombo2) {
		this.codCombo2 = codCombo2;
	}


	
	
	
	
}
