import { useQuery } from "@tanstack/react-query";
import { mulighetsrommetClient } from "../api/clients";
import { QueryKeys } from "../api/QueryKeys";

export function Oversikt() {
  const response = useQuery(QueryKeys.tiltakstyper, () =>
    mulighetsrommetClient.tiltakstyper.getTiltakstyper({})
  );
  console.log(response.data);
  return (
    <div>
      <h1>Oversikt</h1>
      <pre>{JSON.stringify(response.data, null, 2)}</pre>
    </div>
  );
}
