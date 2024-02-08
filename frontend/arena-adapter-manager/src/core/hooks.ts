import { useEffect, useState } from "react";
import { Topic } from "../domain";
import { ApiBase, getArenaTables, getTopics } from "./api";

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
