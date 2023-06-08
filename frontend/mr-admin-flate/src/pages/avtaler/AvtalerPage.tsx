import { Button, Heading } from "@navikt/ds-react";
import { mulighetsrommetClient } from "../../api/clients";
import { Avtalefilter } from "../../components/filter/Avtalefilter";
import { AvtaleTabell } from "../../components/tabell/AvtaleTabell";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { MainContainer } from "../../layouts/MainContainer";
import styles from "../Page.module.scss";
import { createRef } from "react";
import { OpenAPI } from "mulighetsrommet-api-client";
import { APPLICATION_NAME } from "../../constants";

async function lastNedFil() {
  const headers = new Headers();
  headers.append("Nav-Consumer-Id", APPLICATION_NAME);

  if (import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN) {
    headers.append(
      "Authorization",
      `Bearer ${import.meta.env.VITE_MULIGHETSROMMET_API_AUTH_TOKEN}`
    );
  }

  headers.append("accept", "application/json");

  // TODO Kan ta i mot filter i metoden og sende som query-params hvis ønskelig
  return await fetch(`${OpenAPI.BASE}/api/v1/internal/avtaler/excel`, {
    headers,
  });
}

export function AvtalerPage() {
  const link = createRef<any>();

  async function lastNedExcel() {
    if (link?.current?.href) {
      return;
    }

    const excelFil = await lastNedFil();
    const blob = await excelFil.blob();
    const url = URL.createObjectURL(blob);
    console.log(url);
    link.current.download = "avtaler.xlsx";
    link.current.href = url;

    link.current.click();
    URL.revokeObjectURL(url);
  }

  return (
    <MainContainer>
      <ContainerLayout>
        <Heading level="2" size="large" className={styles.header_wrapper}>
          Oversikt over avtaler
        </Heading>
        <Button onClick={lastNedExcel}>Last ned Excel-fil</Button>
        <a ref={link}></a>
        <Avtalefilter />
        <AvtaleTabell />
      </ContainerLayout>
    </MainContainer>
  );
}
