import { ReactNode } from "react";
import styles from "./RootLayout.module.scss";
import { ForsideTiltaksansvarlig } from "../pages/forside/ForsideTiltaksansvarlig";
import { ForsideFagansvarlig } from "../pages/forside/ForsideFagansvarlig";

interface RootLayoutProps {
  fagansvarlig?: boolean;
  children: ReactNode;
}

export function RootLayout({
  fagansvarlig = false,
  children,
}: RootLayoutProps) {
  return (
    <>
      {fagansvarlig ? <ForsideFagansvarlig /> : <ForsideTiltaksansvarlig />}
      <main className={styles.container}>{children}</main>
    </>
  );
}
