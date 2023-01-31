import { ReactNode } from "react";
import styles from "./RootLayout.module.scss";
import { ForsideTiltaksansvarlig } from "../pages/forside/ForsideTiltaksansvarlig";
import { ForsideFagansvarlig } from "../pages/forside/ForsideFagansvarlig";
import { useHentAnsatt } from "../api/ansatt/useHentAnsatt";
import { hentAnsattsRolle } from "../tilgang/tilgang";

interface RootLayoutProps {
  children: ReactNode;
}

export function RootLayout({ children }: RootLayoutProps) {
  const { data, isLoading } = useHentAnsatt();
  if (isLoading) return null;

  const tilgang = hentAnsattsRolle(data);
  return (
    <>
      {tilgang === "FAGANSVARLIG" ? (
        <ForsideFagansvarlig />
      ) : (
        <ForsideTiltaksansvarlig />
      )}
      <main className={styles.container}>{children}</main>
    </>
  );
}
