import { Topic } from "../domain";

export const getTopics = async () =>
  fetch("http://localhost:8084/manager/topics", {
    method: "GET",
  }).then((response) => response.json());

export const putTopicRunningState = async (topics: Topic[]) => {
  return fetch("http://localhost:8084/manager/topics", {
    method: "PUT",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify(topics),
  });
};
