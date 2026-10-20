package com.deliverytest.api.order;

public enum OrderStatus {
    RECEBIDO,
    EM_PREPARO,
    SAIU_PARA_ENTREGA,
    ENTREGUE,
    CANCELADO;

    public boolean canTransitionTo(OrderStatus newStatus) {
        if (newStatus == null) {
            return false;
        }

        return switch (this) {
            case RECEBIDO ->
                    newStatus == EM_PREPARO ||
                            newStatus == CANCELADO;

            case EM_PREPARO ->
                    newStatus == SAIU_PARA_ENTREGA ||
                            newStatus == CANCELADO;

            case SAIU_PARA_ENTREGA ->
                    newStatus == ENTREGUE;

            case ENTREGUE, CANCELADO -> false;
        };
    }
}