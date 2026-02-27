export enum TopicType {
  CONSUMER,
  PRODUCER,
}

export interface Topic {
  id: string;
  type: TopicType;
  topic: string;
  running: boolean;
}

export interface ScheduledTask {
  taskName: string;
  taskInstance: string;
  taskData: string;
  executionTime: string;
  picked: boolean;
  pickedBy: string | null;
  lastSuccess: string | null;
  lastFailure: string | null;
  consecutiveFailures: number;
  lastHeartbeat: string | null;
  version: number;
  priority: number | null;
}
