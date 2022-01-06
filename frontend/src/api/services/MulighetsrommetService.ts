/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Innsatsgruppe } from '../models/Innsatsgruppe';
import type { Tiltaksgjennomforing } from '../models/Tiltaksgjennomforing';
import type { Tiltaksvariant } from '../models/Tiltaksvariant';
import type { UnsavedTiltaksvariant } from '../models/UnsavedTiltaksvariant';
import type { CancelablePromise } from '../core/CancelablePromise';
import { request as __request } from '../core/request';

export class MulighetsrommetService {

    /**
     * @returns Innsatsgruppe Array of innsatsgrupper.
     * @throws ApiError
     */
    public static getInnsatsgrupper(): CancelablePromise<Array<Innsatsgruppe>> {
        return __request({
            method: 'GET',
            path: `/api/innsatsgrupper`,
        });
    }

    /**
     * @returns Tiltaksvariant Array of tiltaksvarianter.
     * @throws ApiError
     */
    public static getTiltaksvarianter(): CancelablePromise<Array<Tiltaksvariant>> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksvarianter`,
        });
    }

    /**
     * @returns Tiltaksvariant The created tiltaksvariant
     * @throws ApiError
     */
    public static createTiltaksvariant({
        requestBody,
    }: {
        requestBody: UnsavedTiltaksvariant,
    }): CancelablePromise<Tiltaksvariant> {
        return __request({
            method: 'POST',
            path: `/api/tiltaksvarianter`,
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * @returns Tiltaksvariant The specified tiltaksvariant.
     * @throws ApiError
     */
    public static getTiltaksvariant({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Tiltaksvariant> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksvarianter/${id}`,
            errors: {
                404: `The specified tiltaksvariant was not found.`,
            },
        });
    }

    /**
     * @returns Tiltaksvariant The updated tiltaksvariant.
     * @throws ApiError
     */
    public static updateTiltaksvariant({
        id,
        requestBody,
    }: {
        /** ID **/
        id: number,
        requestBody: Tiltaksvariant,
    }): CancelablePromise<Tiltaksvariant> {
        return __request({
            method: 'PUT',
            path: `/api/tiltaksvarianter/${id}`,
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                404: `The specified tiltaksvariant was not found.`,
            },
        });
    }

    /**
     * @returns any The delete was successful.
     * @throws ApiError
     */
    public static deleteTiltaksvariant({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<any> {
        return __request({
            method: 'DELETE',
            path: `/api/tiltaksvarianter/${id}`,
            errors: {
                404: `The specified tiltaksvariant was not found.`,
            },
        });
    }

    /**
     * @returns Tiltaksgjennomforing Tiltaksgjennomføringer for the specified tiltaksvariant.
     * @throws ApiError
     */
    public static getTiltaksgjennomforingerByTiltaksvariant({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksvarianter/${id}/tiltaksgjennomforinger`,
        });
    }

    /**
     * @returns Tiltaksgjennomforing Array of tiltaksgjennomføringer.
     * @throws ApiError
     */
    public static getTiltaksgjennomforinger(): CancelablePromise<Array<Tiltaksgjennomforing>> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksgjennomforinger`,
        });
    }

    /**
     * @returns Tiltaksgjennomforing The specified tiltaksgjennomføring.
     * @throws ApiError
     */
    public static getTiltaksgjennomforing({
        id,
    }: {
        /** ID **/
        id: number,
    }): CancelablePromise<Tiltaksgjennomforing> {
        return __request({
            method: 'GET',
            path: `/api/tiltaksgjennomforinger/${id}`,
            errors: {
                404: `The specified tiltaksgjennomføring was not found.`,
            },
        });
    }

}