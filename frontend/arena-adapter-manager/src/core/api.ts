import { Topic } from "../domain";

export const getTopics = async () =>
  fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "GET",
  }).then((response) => response.json());

export const putTopicRunningState = async (topics: Topic[]) => {
  return fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "PUT",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(topics),
  });
};
