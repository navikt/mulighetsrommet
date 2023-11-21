import { Button, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import { FormProvider, useForm } from "react-hook-form";
import {
  defaultTiltakstypeFilter,
  paginationAtom,
  TiltakstypeFilter,
  tiltakstypeFilterAtom,
} from "../../api/atoms";
import { resetPaginering, valueOrDefault } from "../../utils/Utils";
import styles from "./Filter.module.scss";
import { FilterTag } from "./FilterTag";
import { ControlledSokeSelect } from "mulighetsrommet-frontend-common";
import { Tiltakstypekategori, Tiltakstypestatus } from "mulighetsrommet-api-client";

export function Tiltakstypefilter() {
  const [filter, setFilter] = useAtom(tiltakstypeFilterAtom);
  const [, setPage] = useAtom(paginationAtom);

  const form = useForm<TiltakstypeFilter>({
    defaultValues: {
      ...filter,
    },
  });
  const { register, setValue } = form;

  const statusOptions = () => {
    return [
      {
        label: "Aktiv",
        value: "Aktiv",
      },
      {
        label: "Planlagt",
        value: "Planlagt",
      },
      {
        label: "Avsluttet",
        value: "Avsluttet",
      },
      {
        label: "Alle statuser",
        value: "",
      },
    ];
  };

  const kategoriOptions = () => {
    return [
      {
        label: "Gruppetiltak",
        value: "GRUPPE",
      },
      {
        label: "Individuelle tiltak",
        value: "INDIVIDUELL",
      },
      {
        label: "Alle kategorier",
        value: "",
      },
    ];
  };

  return (
    <FormProvider {...form}>
      <form className={styles.tiltakstypeform}>
        <div className={styles.filter_container}>
          <div className={styles.filtrering}>
            <Search
              label="Søk etter tiltakstype"
              hideLabel
              variant="simple"
              onChange={(sok: string) =>
                setFilter({
                  ...filter,
                  sok,
                })
              }
              value={filter.sok}
              aria-label="Søk etter tiltakstype"
              size="small"
              className={styles.form_field}
            />
            <ControlledSokeSelect
              size="small"
              label="Filtrer på statuser"
              placeholder="Filtrer på statuser"
              hideLabel
              {...register("status")}
              className={styles.form_field}
              onChange={(e) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  status: valueOrDefault(
                    e.target.value as Tiltakstypestatus,
                    defaultTiltakstypeFilter.status,
                  ),
                });
              }}
              options={statusOptions()}
            />
            <ControlledSokeSelect
              size="small"
              label="Gruppetiltak eller individuelle tiltak"
              placeholder="Gruppetiltak eller individuelle tiltak"
              hideLabel
              {...register("kategori")}
              onChange={(e) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  kategori: valueOrDefault(
                    e.target.value as Tiltakstypekategori,
                    defaultTiltakstypeFilter.kategori,
                  ),
                });
              }}
              options={kategoriOptions()}
              className={styles.form_field}
            />
          </div>
          <div className={styles.tags_container}>
            {filter.sok && (
              <FilterTag
                label={`'${filter.sok}'`}
                onClick={() => {
                  setFilter({
                    ...filter,
                    sok: "",
                  });
                  setValue("sok", "");
                }}
              />
            )}
            {filter.status && (
              <FilterTag
                label={filter.status}
                onClick={() => {
                  setFilter({
                    ...filter,
                    status: undefined,
                  });
                  setValue("status", undefined);
                }}
              />
            )}
            {filter.kategori && (
              <FilterTag
                label={filter.kategori === "GRUPPE" ? "Gruppetiltak" : "Individuelle tiltak"}
                onClick={() => {
                  setFilter({
                    ...filter,
                    kategori: undefined,
                  });
                  setValue("kategori", undefined);
                }}
              />
            )}
            {(filter.sok ||
              filter.status !== defaultTiltakstypeFilter.status ||
              filter.kategori !== defaultTiltakstypeFilter.kategori) && (
              <Button
                style={{
                  height: "16px",
                  maxHeight: "16px",
                }}
                type="button"
                size="small"
                variant="tertiary"
                onClick={() => {
                  setFilter(defaultTiltakstypeFilter);
                  setValue("status", defaultTiltakstypeFilter.status);
                  setValue("kategori", defaultTiltakstypeFilter.kategori);
                }}
              >
                Tilbakestill filter
              </Button>
            )}
          </div>
        </div>
      </form>
    </FormProvider>
  );
}
