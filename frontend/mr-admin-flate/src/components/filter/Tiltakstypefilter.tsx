import { Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { ChangeEvent } from "react";
import { FormProvider, useForm } from "react-hook-form";
import {
  TiltakstypeFilter,
  paginationAtom,
  tiltakstypeFilter,
} from "../../api/atoms";
import { resetPaginering } from "../../utils/Utils";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./Filter.module.scss";

export function Tiltakstypefilter() {
  const [sokefilter, setSokefilter] = useAtom(tiltakstypeFilter);
  const [, setPage] = useAtom(paginationAtom);

  const form = useForm<TiltakstypeFilter>({
    defaultValues: {
      ...sokefilter,
    },
  });
  const { register } = form;

  const statusOptions = () => {
    return [
      { label: "Aktiv", value: "Aktiv" },
      { label: "Planlagt", value: "Planlagt" },
      { label: "Avsluttet", value: "Avsluttet" },
      { label: "Alle", value: "Alle" },
    ];
  };

  const kategoriOptions = () => {
    return [
      { label: "Gruppetiltak", value: "GRUPPE" },
      { label: "Individuelle tiltak", value: "INDIVIDUELLE" },
      { label: "Alle", value: "ALLE" },
    ];
  };

  return (
    <FormProvider {...form}>
      <form>
        <div className={styles.filter_container}>
          <div className={styles.filter_left}>
            <Search
              label="Søk etter tiltakstype"
              hideLabel
              variant="simple"
              data-testid="filter_sokefelt"
              onChange={(sok: string) => setSokefilter({ ...sokefilter, sok })}
              value={sokefilter.sok}
              aria-label="Søk etter tiltakstype"
              size="small"
              className={styles.form_field}
            />
            <SokeSelect
              label="Filtrer på statuser"
              placeholder="Filtrer på statuser"
              hideLabel
              data-testid="filter_status"
              {...register("status")}
              className={styles.form_field}
              onChange={(e: ChangeEvent<HTMLSelectElement>) => {
                resetPaginering(setPage);
                const status = e.currentTarget.value as any;
                setSokefilter({
                  ...sokefilter,
                  status: status === "Alle" ? undefined : status,
                });
              }}
              options={statusOptions()}
            />

            <SokeSelect
              label="Gruppetiltak eller individuelle tiltak"
              placeholder="Gruppetiltak eller individuelle tiltak"
              hideLabel
              {...register("kategori")}
              data-testid="filter_kategori"
              onChange={(e: ChangeEvent<HTMLSelectElement>) => {
                resetPaginering(setPage);
                const kategori = e.currentTarget.value as any;
                setSokefilter({
                  ...sokefilter,
                  kategori: kategori === "ALLE" ? undefined : kategori,
                });
              }}
              options={kategoriOptions()}
              className={styles.form_field}
            />
          </div>
        </div>
      </form>
    </FormProvider>
  );
}
