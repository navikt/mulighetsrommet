import { Topic } from "../domain";

export const getTopics = async () =>
  fetch("http://0.0.0.0:8084/topics", {
    method: "GET",
    headers: {
      Authorization: `Bearer eyJraWQiOiJhenVyZSIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJhZG1pbkBtdWxpZ2hldHNyb21tZXQiLCJhdWQiOiJtdWxpZ2hldHNyb21tZXQtYXBpIiwibmJmIjoxNjYzNjU3MjgyLCJOQVZpZGVudCI6IkFCQzEyMyIsImlzcyI6Imh0dHA6XC9cL2xvY2FsaG9zdDo4MDgxXC9henVyZSIsImV4cCI6MTY3OTQzNzI4MiwiaWF0IjoxNjYzNjU3MjgyLCJub25jZSI6IjU2NzgiLCJqdGkiOiJkZmVlNTcwNi0xZTRiLTQwNmEtYjEyZi04ZjA2MjY0Zjg4ZTEifQ.eOlRZVMtmwPuXjzVJ6p5UdL10okb_ITmr0YUhOYGLKfYpfJdoAA9bL7OK8mQ6ZcRyDi5y3X-yrZHzYyXUqfv6XA1LzfJWONdYiXIkWk7fm9MZ1J1Kdo8djfhfNSve0sEdY9g-ZMEZMskyO31P5gm3mR1Rmt2h0XJIIMaIRKQs001YRM6A1WchJpFwnHk2nglZPncf0g_Q64SDU8SONMh0Nf6Hzl7AB7GDMgP74Fvlai4wmBj2Fb5LiCZtphItpf7NKy0XonxD6CIF6fWlIcbQXAB5PYNYygr1UM80Ah3MS4W7wmmSAa1CpuHvhuBVW0YmR_DjgCrx_IYLiqoM9_DSQ`,
    },
  }).then((response) => response.json());

export const putTopicRunningState = async (topics: Topic[]) => {
  return fetch("/mulighetsrommet-arena-adapter/topics", {
    method: "PUT",
    headers: {
      "content-type": "application/json",
      Authorization: `Bearer eyJraWQiOiJhenVyZSIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJhZG1pbkBtdWxpZ2hldHNyb21tZXQiLCJhdWQiOiJtdWxpZ2hldHNyb21tZXQtYXBpIiwibmJmIjoxNjYzNjU3MjgyLCJOQVZpZGVudCI6IkFCQzEyMyIsImlzcyI6Imh0dHA6XC9cL2xvY2FsaG9zdDo4MDgxXC9henVyZSIsImV4cCI6MTY3OTQzNzI4MiwiaWF0IjoxNjYzNjU3MjgyLCJub25jZSI6IjU2NzgiLCJqdGkiOiJkZmVlNTcwNi0xZTRiLTQwNmEtYjEyZi04ZjA2MjY0Zjg4ZTEifQ.eOlRZVMtmwPuXjzVJ6p5UdL10okb_ITmr0YUhOYGLKfYpfJdoAA9bL7OK8mQ6ZcRyDi5y3X-yrZHzYyXUqfv6XA1LzfJWONdYiXIkWk7fm9MZ1J1Kdo8djfhfNSve0sEdY9g-ZMEZMskyO31P5gm3mR1Rmt2h0XJIIMaIRKQs001YRM6A1WchJpFwnHk2nglZPncf0g_Q64SDU8SONMh0Nf6Hzl7AB7GDMgP74Fvlai4wmBj2Fb5LiCZtphItpf7NKy0XonxD6CIF6fWlIcbQXAB5PYNYygr1UM80Ah3MS4W7wmmSAa1CpuHvhuBVW0YmR_DjgCrx_IYLiqoM9_DSQ`,
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
    }
  );
};
