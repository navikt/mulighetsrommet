export enum TopicType {
  CONSUMER,
  PRODUCER,
}

export interface Topic {
  id: number;
  key: string;
  type: TopicType;
  topic: string;
  running: boolean;
}
