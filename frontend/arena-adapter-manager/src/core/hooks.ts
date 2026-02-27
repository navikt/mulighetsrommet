import { useEffect, useState } from "react";
import { KafkaConsumerRecord, ScheduledTask, Topic } from "../domain";
import {
  ApiBase,
  getArenaTables,
  getFailedKafkaConsumerRecords,
  getFailedScheduledTasks,
  getTopics,
} from "./api";

export function useTopics(base: ApiBase) {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchTopics = async () => {
      const t = await getTopics(base);
      setTopics(t ?? []);
      setIsLoading(false);
    };
    fetchTopics();
  }, [base]);
  return { topics, isTopicsLoading: isLoading, setTopics };
}

export function useArenaTables() {
  const [arenaTables, setArenaTables] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchArenaTables = async () => {
      const t = await getArenaTables();
      setArenaTables(t ?? []);
      setIsLoading(false);
    };
    fetchArenaTables();
  }, []);
  return { arenaTables, isArenaTablesLoading: isLoading };
}

export function useFailedScheduledTasks(base: ApiBase) {
  const [tasks, setTasks] = useState<ScheduledTask[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchScheduledTasks = async () => {
      const data = await getFailedScheduledTasks(base);
      setTasks(data);
      setIsLoading(false);
    };
    fetchScheduledTasks();
  }, []);
  return { isLoading, tasks };
}

export function useFailedKafkaConsumerRecords(base: ApiBase) {
  const [records, setRecords] = useState<KafkaConsumerRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchScheduledTasks = async () => {
      const data = await getFailedKafkaConsumerRecords(base);
      setRecords(data);
      setIsLoading(false);
    };
    fetchScheduledTasks();
  }, []);
  return { isLoading, records };
}
