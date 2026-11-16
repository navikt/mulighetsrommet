import { useEffect, useState } from "react";
import { KafkaConsumerRecord, ScheduledTask, KafkaTopicConsumer } from "../domain";
import {
  ApiBase,
  getArenaTables,
  getFailedKafkaConsumerRecords,
  getFailedScheduledTasks,
  getKafkaConsumers,
} from "./api";

export function useKafkaConsumers(base: ApiBase) {
  const [consumers, setConsumers] = useState<KafkaTopicConsumer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchConsumers = async () => {
      const consumers = await getKafkaConsumers(base);
      setConsumers(consumers ?? []);
      setIsLoading(false);
    };
    fetchConsumers();
  }, [base]);
  return { consumers, isConsumersLoading: isLoading, setConsumers };
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

  const fetchScheduledTasks = async () => {
    const data = await getFailedScheduledTasks(base);
    setTasks(data);
    setIsLoading(false);
  };

  const refetch = () => {
    setIsLoading(true);
    fetchScheduledTasks();
  };

  useEffect(() => {
    fetchScheduledTasks();
  }, []);
  return { isLoading, tasks, refetch };
}

export function useFailedKafkaConsumerRecords(base: ApiBase) {
  const [records, setRecords] = useState<KafkaConsumerRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchScheduledTasks = async () => {
    const data = await getFailedKafkaConsumerRecords(base);
    setRecords(data);
    setIsLoading(false);
  };
  useEffect(() => {
    fetchScheduledTasks();
  }, []);
  const refetch = () => {
    setIsLoading(true);
    fetchScheduledTasks();
  };
  return { isLoading, records, refetch };
}
