/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.aurora.scheduler.async;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.twitter.common.application.modules.LifecycleModule;
import com.twitter.common.stats.StatsProvider;
import com.twitter.common.testing.easymock.EasyMockTest;
import com.twitter.common.util.Clock;

import org.apache.aurora.scheduler.AppStartup;
import org.apache.aurora.scheduler.async.preemptor.Preemptor;
import org.apache.aurora.scheduler.filter.SchedulingFilter;
import org.apache.aurora.scheduler.mesos.Driver;
import org.apache.aurora.scheduler.state.MaintenanceController;
import org.apache.aurora.scheduler.state.StateManager;
import org.apache.aurora.scheduler.state.TaskAssigner;
import org.apache.aurora.scheduler.storage.Storage;
import org.apache.aurora.scheduler.storage.testing.StorageTestUtil;
import org.apache.aurora.scheduler.testing.FakeStatsProvider;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TODO(wfarner): Make this test more useful by validating the bindings set up by the module.
 * Multibindings makes this tricky since it uses an internal binding annotation which makes a direct
 * check on injector.getBindings() impossible.
 */
public class AsyncModuleTest extends EasyMockTest {

  private FakeStatsProvider statsProvider;
  private StorageTestUtil storageUtil;

  @Before
  public void setUp() {
    statsProvider = new FakeStatsProvider();
    storageUtil = new StorageTestUtil(this);
    storageUtil.expectOperations();
  }

  private Injector createInjector(Module module) {
    return Guice.createInjector(
        module,
        new LifecycleModule(),
        new AbstractModule() {
          private <T> void bindMock(Class<T> clazz) {
            bind(clazz).toInstance(createMock(clazz));
          }

          @Override
          protected void configure() {
            bind(StatsProvider.class).toInstance(statsProvider);
            bindMock(Clock.class);
            bindMock(Driver.class);
            bindMock(SchedulingFilter.class);
            bindMock(MaintenanceController.class);
            bindMock(Preemptor.class);
            bindMock(StateManager.class);
            bindMock(TaskAssigner.class);
            bindMock(Thread.UncaughtExceptionHandler.class);
            bind(Storage.class).toInstance(storageUtil.storage);
          }
        });
  }

  @Test
  public void testBindings() throws Exception {
    Injector injector = createInjector(new AsyncModule());

    control.replay();

    Set<Service> services = injector.getInstance(
        Key.get(new TypeLiteral<Set<Service>>() { }, AppStartup.class));
    for (Service service : services) {
      service.startAsync().awaitRunning();
    }

    injector.getBindings();

    assertEquals(
        ImmutableMap.of(AsyncModule.TIMEOUT_QUEUE_GAUGE, 0, AsyncModule.ASYNC_TASKS_GAUGE, 0L),
        statsProvider.getAllValues()
    );
  }
}
