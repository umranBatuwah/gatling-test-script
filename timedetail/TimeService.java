package com.synectiks.asset.business.service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.synectiks.asset.domain.AccountServices;
import com.synectiks.asset.domain.time;
import com.synectiks.asset.repository.TimeRepository;
import com.synectiks.asset.response.AccountTree;
import com.synectiks.asset.response.App;
import com.synectiks.asset.response.AvailabilityResponse;
import com.synectiks.asset.response.BusinessService;
import com.synectiks.asset.response.Cluster;
import com.synectiks.asset.response.CommonService;
import com.synectiks.asset.response.Data;
import com.synectiks.asset.response.DataProtectionResponse;
import com.synectiks.asset.response.Environment;
import com.synectiks.asset.response.PerformanceResponse;
import com.synectiks.asset.response.Product;
import com.synectiks.asset.response.SecurityResponse;
import com.synectiks.asset.response.UserExperianceResponse;
import com.synectiks.asset.response.Vpc;
import com.synectiks.asset.web.rest.errors.BadRequestAlertException;


@Service
public class TimeService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDetailService.class);


    @Autowired
    private TimeRepository timeRepository;

	@Autowired
	private AccountServicesService accountServicesService;

    public Optional<time> gettimeDetail(Long id) {
		logger.info("Get time detail by id: {}", id);
		return timeRepository.findById(id);
	}

    public List<time> getAlltimeDetail() {
		logger.info("Get all time detail");
		return timeRepository.findAll(Sort.by(Direction.DESC, "id"));
	}

    public Optional<time> deletetimeDetail(Long id) {
		logger.info("Delete service detail by id: {}", id);
		Optional<time> oObj = gettimeDetail(id);
		if (!oObj.isPresent()) {
			logger.warn("Id {} not found. time detail cannot be deleted", id);
			return oObj;
		}
		timeRepository.deleteById(id);
		// transformServiceDetailsListToTree();
		return oObj;
	}

    public time createtimeDetail(time obj) {
		logger.info("Create new time detail");
		time sd = timeRepository.save(obj);
		// transformServiceDetailsListToTree();
		return sd;
	}

    public time updatetimeDetail(time obj) {
		logger.info("Update time detail. Id: {}", obj.getId());
		if (!timeRepository.existsById(obj.getId())) {
			throw new BadRequestAlertException("Entity not found", "timeDetail", "idnotfound");
		}
		time sd = timeRepository.save(obj);
		// transformServiceDetailsListToTree();
		return sd;
	}

	@Transactional
	public List<AccountTree> transformServiceDetailsListToTree() {
		logger.info("Transforming service details to account specific tree");
		List<time> listSd = getAlltimeDetail();
		Map<String, List<time>> acMap = filterAccountSpecificList(listSd);
		List<AccountTree> treeList = filterVpcs(acMap);
		filterClusters(acMap, treeList);
		filterProducts(acMap, treeList);
		filterEnvironments(acMap, treeList);
		filterServiceNature(acMap, treeList);
		filterAppAndDatatime(acMap, treeList);
		logger.info("Service detail transformation completed. Now updating account services");
		logger.debug("Cleaning up account services");
		for (AccountServices as : accountServicesService.getAllAccountServices()) {
			accountServicesService.deleteAccountServices(as.getId());
		}
		logger.debug("Account service clean up completed. Now updating account services");
		Gson gson = new Gson();
		for (AccountTree at : treeList) {
			AccountServices as = AccountServices.builder().accountId(at.getAccount()).build();
			Map<String, Object> attributes = gson.fromJson(gson.toJson(at), Map.class);
			as.setAccount_services_json(attributes);
			accountServicesService.createAccountServices(as);
		}
		logger.info("Account services updated completed");
		return treeList;
	}
	
	private void filterAppAndDatatime(Map<String, List<time>> acMap, List<AccountTree> treeList) {
		for (AccountTree account : treeList) {
			for (com.synectiks.asset.response.Vpc vpc : account.getVpcs()) {
				for (com.synectiks.asset.response.Cluster cluster : vpc.getClusters()) {
					for (com.synectiks.asset.response.Product product : cluster.getProducts()) {
						for (com.synectiks.asset.response.Environment environment : product.getEnvironments()) {
							if (environment.getServices() != null) {
								com.synectiks.asset.response.Service service = environment.getServices();

								if (service.getBusiness() != null) {
									List<App> appList = new ArrayList<>();
									List<Data> dataList = new ArrayList<>();
									for (BusinessService bs : service.getBusiness()) {

										bs.setApp(appList);
										bs.setData(dataList);

										for (Map.Entry<String, List<time>> entry : acMap.entrySet()) {
											if (entry.getKey().equals(account.getAccount())) {
												for (time sd : entry.getValue()) {
													String vpcName = (String) sd.getJoining_time()
															.get("associatedProductEnclave");
													String clusterName = (String) sd.getJoining_time()
															.get("associatedCluster");
													String productName = (String) sd.getJoining_time()
															.get("associatedProduct");
													String envName = (String) sd.getJoining_time()
															.get("associatedEnv");
													String associatedBusinessService = (String) sd.getJoining_time()
															.get("associatedBusinessService");

													if (!StringUtils.isBlank(vpcName)) {
														if (vpcName.substring(vpcName.indexOf("-") + 1)
																.equalsIgnoreCase(vpc.getName())
																&& clusterName
																		.substring(clusterName.lastIndexOf("-") + 1)
																		.equalsIgnoreCase(cluster.getName())
																&& productName.equalsIgnoreCase(product.getName())
																&& envName.equalsIgnoreCase(environment.getName())
																&& associatedBusinessService
																		.equalsIgnoreCase(bs.getName())) {
															String serviceType = (String) sd.getJoining_time()
																	.get("serviceType");
															if (serviceType.equalsIgnoreCase("App")) {
																App app = buildApp(sd, envName);
																bs.getApp().add(app);
															} else if (serviceType.equalsIgnoreCase("Data")) {
																Data data = buildData(sd, envName);
																bs.getData().add(data);
															}
														}
													}
												}
											}
										}
									}
								}
								if (service.getCommon() != null) {
									List<App> appList = new ArrayList<>();
									List<Data> dataList = new ArrayList<>();
									for (CommonService cs : service.getCommon()) {
										cs.setApp(appList);
										cs.setData(dataList);

										for (Map.Entry<String, List<time>> entry : acMap.entrySet()) {
											if (entry.getKey().equals(account.getAccount())) {
												for (time sd : entry.getValue()) {

													String vpcName = (String) sd.getJoining_time()
															.get("associatedProductEnclave");
													String clusterName = (String) sd.getJoining_time()
															.get("associatedCluster");
													String productName = (String) sd.getJoining_time()
															.get("associatedProduct");
													String envName = (String) sd.getJoining_time()
															.get("associatedEnv");
													String associatedCommonService = (String) sd.getJoining_time()
															.get("associatedCommonService");

													if (!StringUtils.isBlank(vpcName)) {
														if (vpcName.substring(vpcName.indexOf("-") + 1)
																.equalsIgnoreCase(vpc.getName())
																&& clusterName
																		.substring(clusterName.lastIndexOf("-") + 1)
																		.equalsIgnoreCase(cluster.getName())
																&& productName.equalsIgnoreCase(product.getName())
																&& envName.equalsIgnoreCase(environment.getName())
																&& associatedCommonService
																		.equalsIgnoreCase(cs.getName())) {
															String serviceType = (String) sd.getJoining_time()
																	.get("serviceType");
															if (serviceType.equalsIgnoreCase("App")) {
																App app = buildApp(sd, envName);
																cs.getApp().add(app);
															} else if (serviceType.equalsIgnoreCase("Data")) {
																Data data = buildData(sd, envName);
																cs.getData().add(data);
															}
														}
													}

												}
											}

											
										}
									}
								}

							}
						}
					}
				}
			}
		}
	}


	private Data buildData(time sd, String envName) {
		Data data = Data.builder().id(envName + "_" + (String) sd.getJoining_time().get("name")).dbid(sd.getId())
				.name((String) sd.getJoining_time().get("name")).serviceDetailId(sd.getId())
				.description((String) sd.getJoining_time().get("description"))
				.associatedCloudElement((String) sd.getJoining_time().get("associatedCloudElement"))
				.associatedClusterNamespace((String) sd.getJoining_time().get("associatedClusterNamespace"))
				.associatedManagedCloudServiceLocation(
						(String) sd.getJoining_time().get("associatedManagedCloudServiceLocation"))
				.associatedGlobalServiceLocation((String) sd.getJoining_time().get("associatedGlobalServiceLocation"))
				.serviceHostingType((String) sd.getJoining_time().get("serviceHostingType"))
				.associatedCloudElementId((String) sd.getJoining_time().get("associatedCloudElementId"))
				.associatedOU((String) sd.getJoining_time().get("associatedOU"))
				.associatedDept((String) sd.getJoining_time().get("associatedDept"))
				.associatedProduct((String) sd.getJoining_time().get("associatedProduct"))
				.associatedEnv((String) sd.getJoining_time().get("associatedEnv"))
				.serviceType((String) sd.getJoining_time().get("serviceType"))

				.performance(PerformanceResponse.builder()
						.score(sd.getJoining_time().get("performance") != null
								? (Integer) ((Map) sd.getJoining_time().get("performance")).get("score")
								: 0)
						.build())
				.availability(AvailabilityResponse.builder()
						.score(sd.getJoining_time().get("availability") != null
								? (Integer) ((Map) sd.getJoining_time().get("availability")).get("score")
								: 0)
						.build())
				.security(SecurityResponse.builder()
						.score(sd.getJoining_time().get("security") != null
								? (Integer) ((Map) sd.getJoining_time().get("security")).get("score")
								: 0)
						.build())
				.dataProtection(DataProtectionResponse.builder()
						.score(sd.getJoining_time().get("dataProtection") != null
								? (Integer) ((Map) sd.getJoining_time().get("dataProtection")).get("score")
								: 0)
						.build())
				.userExperiance(UserExperianceResponse.builder()
						.score(sd.getJoining_time().get("userExperiance") != null
								? (Integer) ((Map) sd.getJoining_time().get("userExperiance")).get("score")
								: 0)
						.build())

				.build();
		return data;
	}

	private App buildApp(time sd, String envName) {
		App app = App.builder().id(envName + "_" + (String) sd.getJoining_time().get("name")).dbid(sd.getId())
				.name((String) sd.getJoining_time().get("name")).serviceDetailId(sd.getId())
				.description((String) sd.getJoining_time().get("description"))
				.associatedCloudElement((String) sd.getJoining_time().get("associatedCloudElement"))
				.associatedClusterNamespace((String) sd.getJoining_time().get("associatedClusterNamespace"))
				.associatedManagedCloudServiceLocation(
						(String) sd.getJoining_time().get("associatedManagedCloudServiceLocation"))
				.associatedGlobalServiceLocation((String) sd.getJoining_time().get("associatedGlobalServiceLocation"))
				.serviceHostingType((String) sd.getJoining_time().get("serviceHostingType"))
				.associatedCloudElementId((String) sd.getJoining_time().get("associatedCloudElementId"))

				.associatedOU((String) sd.getJoining_time().get("associatedOU"))
				.associatedDept((String) sd.getJoining_time().get("associatedDept"))
				.associatedProduct((String) sd.getJoining_time().get("associatedProduct"))
				.associatedEnv((String) sd.getJoining_time().get("associatedEnv"))
				.serviceType((String) sd.getJoining_time().get("serviceType"))
				.performance(PerformanceResponse.builder()
						.score(sd.getJoining_time().get("performance") != null
								? (Integer) ((Map) sd.getJoining_time().get("performance")).get("score")
								: 0)
						.build())
				.availability(AvailabilityResponse.builder()
						.score(sd.getJoining_time().get("availability") != null
								? (Integer) ((Map) sd.getJoining_time().get("availability")).get("score")
								: 0)
						.build())
				.security(SecurityResponse.builder()
						.score(sd.getJoining_time().get("security") != null
								? (Integer) ((Map) sd.getJoining_time().get("security")).get("score")
								: 0)
						.build())
				.dataProtection(DataProtectionResponse.builder()
						.score(sd.getJoining_time().get("dataProtection") != null
								? (Integer) ((Map) sd.getJoining_time().get("dataProtection")).get("score")
								: 0)
						.build())
				.userExperiance(UserExperianceResponse.builder()
						.score(sd.getJoining_time().get("userExperiance") != null
								? (Integer) ((Map) sd.getJoining_time().get("userExperiance")).get("score")
								: 0)
						.build())

				.build();
		return app;
	}
	
	

	private void filterServiceNature(Map<String, List<time>> acMap, List<AccountTree> treeList) {
		for (AccountTree account : treeList) {
			for (Vpc vpc : account.getVpcs()) {
				for (Cluster cluster : vpc.getClusters()) {
					for (Product product : cluster.getProducts()) {
						for (Environment environment : product.getEnvironments()) {
							com.synectiks.asset.response.Service service = com.synectiks.asset.response.Service
									.builder().build();
							List<BusinessService> businessServiceList = new ArrayList<>();
							List<CommonService> commonServiceList = new ArrayList<>();
							service.setBusiness(businessServiceList);
							service.setCommon(commonServiceList);
							Map<String, BusinessService> bsMap = new HashMap();
							Map<String, CommonService> csMap = new HashMap();

							for (Map.Entry<String, List<time>> entry : acMap.entrySet()) {
								if (entry.getKey().equals(account.getAccount())) {
									for (time sd : entry.getValue()) {
										String vpcName = (String) sd.getJoining_time().get("associatedProductEnclave");
										String clusterName = (String) sd.getJoining_time().get("associatedCluster");
										String productName = (String) sd.getJoining_time().get("associatedProduct");
										String envName = (String) sd.getJoining_time().get("associatedEnv");
										if (!StringUtils.isBlank(vpcName)) {
											if (vpcName.substring(vpcName.indexOf("-") + 1)
													.equalsIgnoreCase(vpc.getName())
													&& clusterName.substring(clusterName.lastIndexOf("-") + 1)
															.equalsIgnoreCase(cluster.getName())
													&& productName.equalsIgnoreCase(product.getName())
													&& envName.equalsIgnoreCase(environment.getName())) {

												String serviceNature = (String) sd.getJoining_time()
														.get("serviceNature");
												if (serviceNature.equalsIgnoreCase("Business")) {
													String associatedBusinessService = (String) sd.getJoining_time()
															.get("associatedBusinessService");
//													String description = (String)sd.getMetadata_json().get("description");
													String associatedOU = (String) sd.getJoining_time()
															.get("associatedOU");
													String associatedDept = (String) sd.getJoining_time()
															.get("associatedDept");

													BusinessService bs = BusinessService.builder()
															.name(associatedBusinessService)
//															.description(description)
															.associatedOU(associatedOU).associatedDept(associatedDept)
															.build();
//													service.getBusiness().add(bs);
													bsMap.put(associatedBusinessService, bs);
												} else if (serviceNature.equalsIgnoreCase("Common")) {
													String associatedCommonService = (String) sd.getJoining_time()
															.get("associatedCommonService");
//													String description = (String)sd.getMetadata_json().get("description");
													String associatedOU = (String) sd.getJoining_time()
															.get("associatedOU");
													String associatedDept = (String) sd.getJoining_time()
															.get("associatedDept");

													CommonService cs = CommonService.builder()
															.name(associatedCommonService)
//															.description(description)
															.associatedOU(associatedOU).associatedDept(associatedDept)
															.build();
//													service.getCommon().add(cs);
													csMap.put(associatedCommonService, cs);
												}

											}
										}
									}
								}
							}
							for (Map.Entry<String, BusinessService> entry : bsMap.entrySet()) {
								service.getBusiness().add(entry.getValue());
							}
							for (Map.Entry<String, CommonService> entry : csMap.entrySet()) {
								service.getCommon().add(entry.getValue());
							}
							environment.setServices(service);
						}
					}
				}
			}
		}
	}

	private void filterEnvironments(Map<String, List<time>> acMap, List<AccountTree> treeList) {
		for (AccountTree account : treeList) {
			for (Vpc vpc : account.getVpcs()) {
				for (Cluster cluster : vpc.getClusters()) {
					for (Product product : cluster.getProducts()) {
						List<Environment> environmentList = new ArrayList<>();
						for (Map.Entry<String, List<time>> entry : acMap.entrySet()) {
							if (entry.getKey().equals(account.getAccount())) {
								for (time sd : entry.getValue()) {
									String vpcName = (String) sd.getJoining_time().get("associatedProductEnclave");
									String clusterName = (String) sd.getJoining_time().get("associatedCluster");
									String productName = (String) sd.getJoining_time().get("associatedProduct");
									if (!StringUtils.isBlank(vpcName)) {
										if (vpcName.substring(vpcName.indexOf("-") + 1).equalsIgnoreCase(vpc.getName())
												&& clusterName.substring(clusterName.lastIndexOf("-") + 1)
														.equalsIgnoreCase(cluster.getName())
												&& productName.equalsIgnoreCase(product.getName())) {
											String envName = (String) sd.getJoining_time().get("associatedEnv");
											Environment env = Environment.builder().name(envName).build();
											if (!environmentList.contains(env)) {
												environmentList.add(env);
											}
										}
									}
								}
							}
						}
						product.setEnvironments(environmentList);
					}
				}
			}
		}
	}

	private void filterProducts(Map<String, List<time>> acMap, List<AccountTree> treeList) {
		for (AccountTree account : treeList) {
			for (Vpc vpc : account.getVpcs()) {
				for (Cluster cluster : vpc.getClusters()) {
					List<Product> productList = new ArrayList<>();
					for (Map.Entry<String, List<time>> entry : acMap.entrySet()) {
						if (entry.getKey().equals(account.getAccount())) {
							for (time sd : entry.getValue()) {
								String vpcName = (String) sd.getJoining_time().get("associatedProductEnclave");
								String clusterName = (String) sd.getJoining_time().get("associatedCluster");
								if (!StringUtils.isBlank(vpcName)) {
									if (vpcName.substring(vpcName.indexOf("-") + 1).equalsIgnoreCase(vpc.getName())
											&& clusterName.substring(clusterName.lastIndexOf("-") + 1)
													.equalsIgnoreCase(cluster.getName())) {
										String productName = (String) sd.getJoining_time().get("associatedProduct");
										Product product = Product.builder().name(productName).build();
										if (!productList.contains(product)) {
											productList.add(product);
										}
									}
								}
							}
						}
					}
					cluster.setProducts(productList);
				}
			}
		}
	}

	private void filterClusters(Map<String, List<time>> acMap, List<AccountTree> treeList) {
		for (Map.Entry<String, List<time>> entry : acMap.entrySet()) {
			for (AccountTree at : treeList) {
				if (entry.getKey().equals(at.getAccount())) {
					for (Vpc vpc : at.getVpcs()) {
						List<Cluster> clusterList = new ArrayList<>();
						for (time sd : entry.getValue()) {
							String vpcName = (String) sd.getJoining_time().get("associatedProductEnclave");
							if (!StringUtils.isBlank(vpcName)) {
								if("Cluster".equalsIgnoreCase((String) sd.getJoining_time().get("serviceHostingType"))) {
									if (vpcName.substring(vpcName.indexOf("-") + 1).equalsIgnoreCase(vpc.getName())) {
										String clusterName = (String) sd.getJoining_time().get("associatedCluster");
										Cluster cl = Cluster.builder().name(clusterName.substring(clusterName.lastIndexOf("-") + 1)).build();
										if (!clusterList.contains(cl)) {
											clusterList.add(cl);
										}
									}
								}else {
									String clusterName = (String) sd.getJoining_time().get("serviceHostingType");
									Cluster cl = Cluster.builder().name(clusterName).build();
									if (!clusterList.contains(cl)) {
										clusterList.add(cl);
									}
								}
								
							}
						}
						vpc.setClusters(clusterList);
					}
				}
			}
		}
	}

	private List<AccountTree> filterVpcs(Map<String, List<time>> acMap) {
		List<AccountTree> treeList = new ArrayList<>();
		for (Map.Entry<String, List<time>> entry : acMap.entrySet()) {
			AccountTree tree = new AccountTree();
			tree.setAccount(entry.getKey());
			List<Vpc> vpcList = new ArrayList<>();
			for (time vpc : entry.getValue()) {
				String name = (String) vpc.getJoining_time().get("associatedProductEnclave");
				if (!StringUtils.isBlank(name)) {
					Vpc v = new Vpc();
					v.setName(name.substring(name.indexOf("-") + 1));
					if (!vpcList.contains(v)) {
						vpcList.add(v);
					}
				}
			}
			tree.setVpcs(vpcList);
			treeList.add(tree);
		}
		return treeList;
	}

	private Map<String, List<time>> filterAccountSpecificList(List<time> listSd) {
		Map<String, List<time>> acMap = new HashMap<>();
		for (time sd : listSd) {
			if (acMap.containsKey((String) sd.getJoining_time().get("associatedLandingZone"))) {
				acMap.get((String) sd.getJoining_time().get("associatedLandingZone")).add(sd);
			} else {
				List<time> list = new ArrayList<>();
				list.add(sd);
				acMap.put((String) sd.getJoining_time().get("associatedLandingZone"), list);
			}
		}
		return acMap;
	}
}
