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
