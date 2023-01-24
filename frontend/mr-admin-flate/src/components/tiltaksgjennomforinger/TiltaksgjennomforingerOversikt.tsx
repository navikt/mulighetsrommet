import {Alert, Pagination} from "@navikt/ds-react";
import {useAtom} from "jotai";
import {paginationAtom} from "../../api/atoms";
import {useTiltaksgjennomforinger} from "../../api/tiltaksgjennomforing/useTiltaksgjennomforinger";
import {PAGE_SIZE} from "../../constants";
import {Laster} from "../Laster";
import {PagineringsOversikt} from "../paginering/PagineringOversikt";
import {TiltaksgjennomforingRad} from "./TiltaksgjennomforingRad";
import styles from "./TiltaksgjennomforingerOversikt.module.scss";
import {ListeheaderTiltaksgjennomforing} from "./ListeheaderTiltaksgjennomforing";

export function TiltaksgjennomforingerOversikt() {
    const {data, isLoading} = useTiltaksgjennomforinger();
    const [page, setPage] = useAtom(paginationAtom);

    if (isLoading) {
        return <Laster size="xlarge"/>;
    }

    if (!data) {
        return null;
    }

    const {data: tiltaksgjennomforinger, pagination: paginering} = data;

    return (
        <>
            {tiltaksgjennomforinger.length > 0 ? (
                <PagineringsOversikt
                    page={page}
                    antall={tiltaksgjennomforinger.length}
                    maksAntall={data.pagination.totalCount}
                />
            ) : null}

            {tiltaksgjennomforinger.length === 0 ? (
                    <Alert variant="info">Vi fant ingen tiltaksgjennomføringer</Alert>
                ) :
                <>
                    <ul className={styles.oversikt}>
                        <ListeheaderTiltaksgjennomforing/>
                        {tiltaksgjennomforinger
                            .sort((a, b) => a.navn.localeCompare(b.navn))
                            .map((tiltaksgjennomforing) => (
                                <TiltaksgjennomforingRad
                                    key={tiltaksgjennomforing.id}
                                    tiltaksgjennomforing={tiltaksgjennomforing}
                                />
                            ))}
                    </ul>
                    <div className={styles.under_oversikt}>
                        {tiltaksgjennomforinger.length > 0 ? (
                            <>
                                <PagineringsOversikt
                                    page={page}
                                    antall={tiltaksgjennomforinger.length}
                                    maksAntall={data.pagination.totalCount}
                                />
                                <Pagination
                                    size="small"
                                    data-testid="paginering"
                                    page={page}
                                    onPageChange={setPage}
                                    count={Math.ceil(
                                        (paginering?.totalCount ?? PAGE_SIZE) / PAGE_SIZE
                                    )}
                                    data-version="v1"
                                />
                            </>
                        ) : null}
                    </div>
                </>}
        </>
    );
}
