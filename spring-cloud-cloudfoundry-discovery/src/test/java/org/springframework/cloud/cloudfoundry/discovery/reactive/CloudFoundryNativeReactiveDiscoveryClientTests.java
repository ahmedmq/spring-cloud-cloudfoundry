/*
 * Copyright 2019-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.cloudfoundry.discovery.reactive;

import java.util.UUID;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.cloudfoundry.CloudFoundryService;
import org.springframework.cloud.cloudfoundry.discovery.CloudFoundryDiscoveryProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Tim Ysewyn
 */
@ExtendWith(MockitoExtension.class)
class CloudFoundryNativeReactiveDiscoveryClientTests {

	@Mock
	private CloudFoundryOperations operations;

	@Mock
	private CloudFoundryService svc;

	@Mock
	private CloudFoundryDiscoveryProperties properties;

	@InjectMocks
	private CloudFoundryNativeReactiveDiscoveryClient client;

	@Test
	public void verifyDefaults() {
		when(properties.getOrder()).thenReturn(0);
		assertThat(client.description())
				.isEqualTo("CF Reactive Service Discovery Client");
		assertThat(client.getOrder()).isEqualTo(0);
	}

	@Test
	public void shouldReturnFluxOfServices() {
		Applications apps = mock(Applications.class);
		when(operations.applications()).thenReturn(apps);
		ApplicationSummary summary = ApplicationSummary.builder()
				.id(UUID.randomUUID().toString()).instances(1).memoryLimit(1024)
				.requestedState("requestedState").diskQuota(1024).name("service")
				.runningInstances(1).build();
		when(apps.list()).thenReturn(Flux.just(summary));
		Flux<String> services = this.client.getServices();
		StepVerifier.create(services).expectNext("service").expectComplete().verify();
	}

	@Test
	public void shouldReturnEmptyFluxForNonExistingService() {
		when(svc.getApplicationInstances("service")).thenReturn(Flux.empty());
		Flux<ServiceInstance> instances = this.client.getInstances("service");
		StepVerifier.create(instances).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void shouldReturnFluxOfServiceInstances() {
		ApplicationDetail applicationDetail = ApplicationDetail.builder()
				.id(UUID.randomUUID().toString()).stack("stack").instances(1)
				.memoryLimit(1024).requestedState("requestedState").diskQuota(1024)
				.name("service").runningInstances(1).build();
		InstanceDetail instanceDetail = InstanceDetail.builder().index("0").build();
		Tuple2<ApplicationDetail, InstanceDetail> instance = Tuples.of(applicationDetail,
				instanceDetail);
		when(this.svc.getApplicationInstances("service")).thenReturn(Flux.just(instance));
		Flux<ServiceInstance> instances = this.client.getInstances("service");
		StepVerifier.create(instances).expectNextCount(1).expectComplete().verify();
	}

}
