package com.twitter.aurora.scheduler.state;

import java.util.Set;

import com.google.common.base.Optional;

import org.apache.mesos.Protos.SlaveID;

import com.twitter.aurora.gen.AssignedTask;
import com.twitter.aurora.gen.ScheduleStatus;
import com.twitter.aurora.gen.TaskConfig;
import com.twitter.aurora.gen.TaskQuery;

/**
 * Thin interface for the state manager.
 */
public interface StateManager {

  /**
   * Performs a simple state change, transitioning all tasks matching a query to the given
   * state and applying the given audit message.
   * TODO(William Farner): Consider removing the return value.
   *
   * @param query Query to perform, the results of which will be modified.
   * @param newState State to move the resulting tasks into.
   * @param auditMessage Audit message to apply along with the state change.
   * @return the number of successful state changes.
   */
  int changeState(
      TaskQuery query,
      ScheduleStatus newState,
      Optional<String> auditMessage);

  /**
   * Assigns a task to a specific slave.
   * This will modify the task record to reflect the host assignment and return the updated record.
   *
   * @param taskId ID of the task to mutate.
   * @param slaveHost Host name that the task is being assigned to.
   * @param slaveId ID of the slave that the task is being assigned to.
   * @param assignedPorts Ports on the host that are being assigned to the task.
   * @return The updated task record, or {@code null} if the task was not found.
   */
  AssignedTask assignTask(
      String taskId,
      String slaveHost,
      SlaveID slaveId,
      Set<Integer> assignedPorts);

  /**
   * Inserts new tasks into the store. Tasks will immediately move into PENDING and will be eligible
   * for scheduling.
   *
   * @param tasks Tasks to insert.
   */
  void insertPendingTasks(Set<TaskConfig> tasks);

  /**
   * Deletes records of tasks from the task store.
   * This will not perform any state checking or state transitions, but will immediately remove
   * the tasks from the store.  It will also silently ignore attempts to delete task IDs that do
   * not exist.
   *
   * @param taskIds IDs of tasks to delete.
   */
  void deleteTasks(final Set<String> taskIds);
}