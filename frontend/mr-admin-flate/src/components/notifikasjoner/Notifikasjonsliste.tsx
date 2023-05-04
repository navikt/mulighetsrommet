import { Heading, Loader } from "@navikt/ds-react";
import { useHentAnsatt } from "../../api/administrator/useHentAdministrator";
import { useFeatureToggles } from "../../api/features/feature-toggles";
import styles from "./BrukerNotifikasjoner.module.scss";
import { Varsel } from "./Varsel";
import { useNotifikasjonerForAnsatt } from "../../api/notifikasjoner/useNotifikasjonerForAnsatt";

export function Notifikasjonsliste() {
  const { data: features } = useFeatureToggles();
  const { data: bruker } = useHentAnsatt();
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt();

  if (!features?.["mulighetsrommet.admin-flate-se-notifikasjoner"]) return null;

  if (isLoading) {
    return <Loader />;
  }

  if (!paginertResultat) {
    return null;
  }

  console.log(paginertResultat);

  const { pagination, data } = paginertResultat;

  return (
    <section className={styles.container}>
      {data.map((n) => {
        return <div>{n.title}</div>;
      })}
    </section>
  );
}
