import { useEffect, useState } from "react";
import { Topic } from "../domain";
import { getArenaTables, getTopics } from "./api";

export function useTopics() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchTopics = async () => {
      const t = await getTopics();
      setTopics(t ?? []);
      setIsLoading(false);
    };
    fetchTopics();
  }, []);
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
