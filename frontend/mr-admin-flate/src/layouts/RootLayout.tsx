import { ReactNode } from "react";
import { ForsideFagansvarlig } from "../pages/forside/ForsideFagansvarlig";
import styles from "./RootLayout.module.scss";

interface RootLayoutProps {
  children: ReactNode;
}

export function RootLayout({ children }: RootLayoutProps) {
  return (
    <>
      <ForsideFagansvarlig />
      <main className={styles.container}>{children}</main>
    </>
  );
}
