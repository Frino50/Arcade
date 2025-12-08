<template>
    <div class="empty-state">
        <div class="empty-icon">🐉</div>
        <p>Aucun monstre détecté dans la base de données.</p>
        <small>Importez un fichier pour commencer votre collection.</small>
    </div>
</template>
<style scoped>
.empty-state {
    text-align: center;
    padding: 4rem;
    color: #64748b;
}

.empty-icon {
    font-size: 4rem;
    margin-bottom: 1rem;
    opacity: 0.5;
}
</style>
