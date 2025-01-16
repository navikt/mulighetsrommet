import { HeaderBanner } from "@/layouts/HeaderBanner";
import { useTitle } from "@mr/frontend-common";
import { BellDotFillIcon } from "@navikt/aksel-icons";
import { Tabs } from "@navikt/ds-react";
import { Outlet, useLoaderData, useLocation, useNavigate } from "react-router";
import { notifikasjonLoader } from "./notifikasjonerLoader";
import { ContentBox } from "@/layouts/ContentBox";

export function NotifikasjonerPage() {
  const { pathname } = useLocation();
  const { leste, uleste } = useLoaderData<typeof notifikasjonLoader>();
  const navigate = useNavigate();
  useTitle("Notifikasjoner");

  return (
    <main>
      <HeaderBanner
        heading="Notifikasjoner"
        harUndermeny
        ikon={
          <BellDotFillIcon
            title="Notifikasjoner"
            className="text-[#FFC166] inline-block text-[3rem]"
          />
        }
      />
      <Tabs value={pathname.includes("tidligere") ? "tidligere" : "nye"} selectionFollowsFocus>
        <Tabs.List id="fane_liste" className="p-[0 0.5rem] w-[1920px] flex items-start m-auto">
          <Tabs.Tab
            value="nye"
            label={`Nye notifikasjoner ${uleste?.pagination.totalCount ? `(${uleste?.pagination.totalCount})` : ""}`}
            onClick={() => navigate("/notifikasjoner")}
            aria-controls="panel"
          />
          <Tabs.Tab
            value="tidligere"
            label={`Tidligere notifikasjoner ${leste?.pagination.totalCount ? `(${leste?.pagination.totalCount})` : ""}`}
            onClick={() => navigate("/notifikasjoner/tidligere")}
            aria-controls="panel"
          />
        </Tabs.List>
        <ContentBox>
          <div id="panel">
            <Outlet />
          </div>
        </ContentBox>
      </Tabs>
    </main>
  );
}
