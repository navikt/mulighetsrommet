import { Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { ChangeEvent, useEffect } from "react";
import { FormProvider, useForm } from "react-hook-form";
import {
  TiltakstypeFilter,
  paginationAtom,
  tiltakstypeFilter,
} from "../../api/atoms";
import { resetPaginering } from "../../utils/Utils";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./Filter.module.scss";
import { RESET } from "jotai/vanilla/utils";

export function Tiltakstypefilter() {
  const [filter, setFilter] = useAtom(tiltakstypeFilter);
  const [, setPage] = useAtom(paginationAtom);

  const form = useForm<TiltakstypeFilter>({
    defaultValues: {
      ...filter,
    },
  });
  const { register } = form;

  useEffect(() => {
    // Reset filter når vi unmounter
    return () => setFilter(RESET);
  }, []);

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
      { label: "Individuelle tiltak", value: "INDIVIDUELL" },
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
              onChange={(sok: string) => setFilter({ ...filter, sok })}
              value={filter.sok}
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
              onChange={(e) => {
                resetPaginering(setPage);
                const status = e as any;
                setFilter({
                  ...filter,
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
              onChange={(e) => {
                resetPaginering(setPage);
                const kategori = e as any;
                setFilter({
                  ...filter,
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
