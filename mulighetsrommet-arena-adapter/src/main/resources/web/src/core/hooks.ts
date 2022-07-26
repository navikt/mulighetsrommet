import { useState, useEffect } from "react";
import { Topic } from "../domain";
import { getTopics } from "./api";

export function useTopics() {
  const [topics, setTopics] = useState<Topic[]>([]);
  const [isTopicsLoading, setIsLoading] = useState(true);
  useEffect(() => {
    const fetchedTopics = async () => {
      const t = await getTopics();
      setTopics(t);
      setIsLoading(false);
    };
    fetchedTopics();
  }, []);
  return { topics, isTopicsLoading, setTopics };
}
