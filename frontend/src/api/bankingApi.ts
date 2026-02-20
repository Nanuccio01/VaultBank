import { http } from "./http";

export type MeResp = {
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
    iban: string;
    balance: string; // arriva come JSON number/string; lo gestiamo come string per sicurezza
};

export type TransferReq = {
    toIban: string;
    amount: number;
};

export type TransferResp = {
    id: string;
    createdAt: string;
    newBalance: string;
};

export type TransferItem = {
    id: string;
    toIban: string;
    amount: string;
    createdAt: string;
};

export async function me(): Promise<MeResp> {
    const { data } = await http.get<MeResp>("/api/banking/me");
    return data;
}

export async function transfer(req: TransferReq): Promise<TransferResp> {
    const { data } = await http.post<TransferResp>("/api/banking/transfer", req);
    return data;
}

export async function latestTransfers(): Promise<TransferItem[]> {
    const { data } = await http.get<TransferItem[]>("/api/banking/transfers");
    return data;
}