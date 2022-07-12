export enum TopicType {
  CONSUMER,
  PRODUCER,
}

export interface Topic {
  id: number;
  name: string;
  type: TopicType;
  topic: string;
  running: boolean;
}
