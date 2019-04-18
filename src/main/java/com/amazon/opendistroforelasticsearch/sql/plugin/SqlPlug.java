/*
 *   Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package com.amazon.opendistroforelasticsearch.sql.plugin;

import com.amazon.opendistroforelasticsearch.sql.executor.AsyncRestExecutor;
import com.amazon.opendistroforelasticsearch.sql.esdomain.LocalClusterState;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.threadpool.ExecutorBuilder;
import org.elasticsearch.threadpool.FixedExecutorBuilder;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class SqlPlug extends Plugin implements ActionPlugin {

	/** Sql plugin specific settings in ES cluster settings */
	private final SqlSettings sqlSettings = new SqlSettings();

	public SqlPlug() {
	}


	public String name() {
		return "sql";
	}

	public String description() {
		return "Use sql to query elasticsearch.";
	}


	@Override
	public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings, IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter, IndexNameExpressionResolver indexNameExpressionResolver, Supplier<DiscoveryNodes> nodesInCluster) {
	    LocalClusterState.state().setResolver(indexNameExpressionResolver);
		return Collections.singletonList(new RestSqlAction(settings, restController));
	}

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService, ScriptService scriptService, NamedXContentRegistry xContentRegistry, Environment environment, NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry) {
	    LocalClusterState.state().setClusterService(clusterService);
	    LocalClusterState.state().setSqlSettings(sqlSettings);
	    return super.createComponents(client, clusterService, threadPool, resourceWatcherService, scriptService, xContentRegistry, environment, nodeEnvironment, namedWriteableRegistry);
    }

	@Override
	public List<ExecutorBuilder<?>> getExecutorBuilders(Settings settings) {
        return Collections.singletonList(
			new FixedExecutorBuilder(
			    settings,
                AsyncRestExecutor.SQL_WORKER_THREAD_POOL_NAME,
                EsExecutors.numberOfProcessors(settings),
                1000,
                null
            )
        );
	}

	@Override
	public List<Setting<?>> getSettings() {
	    return sqlSettings.getSettings();
	}
}
