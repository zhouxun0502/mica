/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.ribbon.rule;

import com.google.common.base.Optional;
import com.netflix.loadbalancer.*;
import net.dreamlu.mica.ribbon.predicate.DiscoveryEnabledPredicate;
import org.springframework.util.Assert;

import java.util.List;

/**
 * ribbon 路由规则
 *
 * @author L.cm
 */
public abstract class DiscoveryEnabledRule extends PredicateBasedRule {
	private final CompositePredicate predicate;

	public DiscoveryEnabledRule(DiscoveryEnabledPredicate discoveryEnabledPredicate) {
		Assert.notNull(discoveryEnabledPredicate, "Parameter 'discoveryEnabledPredicate' can't be null");
		this.predicate = createCompositePredicate(discoveryEnabledPredicate, new AvailabilityPredicate(this, null));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractServerPredicate getPredicate() {
		return predicate;
	}

	@Override
	public Server choose(Object key) {
		ILoadBalancer lb = getLoadBalancer();

		List<Server> allServers = lb.getAllServers();
		// 过滤服务列表
		List<Server> serverList = filterServers(allServers);

		Optional<Server> server = getPredicate().chooseRoundRobinAfterFiltering(serverList, key);
		if (server.isPresent()) {
			return server.get();
		} else {
			return null;
		}
	}

	/**
	 * 过滤服务
	 *
	 * @param serverList 服务列表
	 * @return 服务列表
	 */
	public abstract List<Server> filterServers(List<Server> serverList);

	private CompositePredicate createCompositePredicate(DiscoveryEnabledPredicate discoveryEnabledPredicate, AvailabilityPredicate availabilityPredicate) {
		RoundRobinRule roundRobinRule =new RoundRobinRule();
		roundRobinRule.setLoadBalancer(getLoadBalancer());
		return CompositePredicate.withPredicates(discoveryEnabledPredicate, availabilityPredicate)
			.addFallbackPredicate(new AvailabilityPredicate(roundRobinRule,null))
			.build();
	}
}
