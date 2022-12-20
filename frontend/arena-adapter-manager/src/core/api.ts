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

export const replayEvents = async (
  arenaTable: string | null,
  consumptionStatus: string | null
) => {
  return await fetch("/mulighetsrommet-arena-adapter/api/topics/replay", {
    method: "PUT",
    headers: {
      "content-type": "application/json",
    },
    body: JSON.stringify({
      table: arenaTable,
      status: consumptionStatus,
    }),
  });
};

export const replayEvent = async (arenaTable: string, arenaId: string) => {
  return await fetch(
    `/mulighetsrommet-arena-adapter/api/topics/replay/${arenaId}?table=${arenaTable}`,
    {
      method: "PUT",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        table: arenaTable,
        arenaId: arenaId,
      }),
    }
  );
};
