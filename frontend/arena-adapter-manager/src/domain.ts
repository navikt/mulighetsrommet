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

export interface KafkaConsumerRecord {
  id: number;
  topic: string;
  partition: number;
  recordOffset: number;
  retries: number;
  lastRetry: string | null;
  key: string | null;
  value: string | null;
  headersJson: string | null;
  recordTimestamp: string | null;
  createdAt: string;
}
