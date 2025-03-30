; -----------------------------------------------
; Name : Draw Nodal network V2 with root creation
; Date : (C)2025
; Site : https://github.com/BlackCreepyCat
; -----------------------------------------------

Graphics 1920,1080,0,2
SetBuffer BackBuffer()

; Définition du type NodalNet
Type NodalNet
    Field NType%        ; 0 = Point de liaison, 1 = Valeur
    Field Radius%       ; Rayon du cercle pour les nodes de valeur
    Field Parent.NodalNet ; Node parent
    Field Value#        ; Valeur du node
    Field Px%          ; Position X
    Field Py%          ; Position Y
    Field Caption$     ; Légende
    Field TargetX#     ; Position X cible pour le déplacement doux
    Field TargetY#     ; Position Y cible pour le déplacement doux
End Type

; Variables globales pour la caméra
Global cam_zoom# = 1.0
Global cam_target_zoom# = 1.0 ; Zoom cible pour le zoom doux
Global cam_x# = 0
Global cam_y# = 0
Global cam_target_x# = 0    ; Position X cible pour la caméra
Global cam_target_y# = 0    ; Position Y cible pour la caméra
Global mouse_down = False
Global last_mx%, last_my%
Global selected_node.NodalNet = Null ; Node actuellement sélectionné
Global last_selected.NodalNet = Null ; Dernier node sélectionné

; Création du réseau de test
Function CreateTestNetwork()
    root.NodalNet = New NodalNet
    root\NType = 0
    root\Px = 0
    root\Py = 0
    root\Caption = "Root"
    root\TargetX = 0
    root\TargetY = 0
    
    For i = 1 To 15
        node.NodalNet = New NodalNet
        node\NType = 1
        node\Value = Rnd(10,100)
        node\Radius = node\Value / 2
        node\Parent = root
        node\Caption = "Node" + i
        angle# = i * 24
        dist# = 50 + i * 20
        node\Px = root\Px + Cos(angle) * dist
        node\Py = root\Py + Sin(angle) * dist
        node\TargetX = node\Px
        node\TargetY = node\Py
        last_selected = node ; Sélectionner le dernier node créé
    Next
End Function

; Vérifie si un node a des enfants
Function HasChildren(node.NodalNet)
    For child.NodalNet = Each NodalNet
        If child\Parent = node Then Return True
    Next
    Return False
End Function

Function DeleteNodeAndChildren(node.NodalNet)
    For child.NodalNet = Each NodalNet
        If child\Parent = node Then DeleteNodeAndChildren(child)
    Next
    Delete node
End Function

; Fonction pour vérifier si la souris est sur un node
Function CheckNodeUnderMouse.NodalNet()
    mx# = (MouseX() - 400) / cam_zoom - cam_x
    my# = (MouseY() - 300) / cam_zoom - cam_y
    
    For node.NodalNet = Each NodalNet
        dx# = node\Px - mx
        dy# = node\Py - my
        dist# = Sqr(dx*dx + dy*dy)
        If node\NType = 1 Then
            If dist < node\Radius Then Return node
        ElseIf node\NType = 0 Then
            If dist < 10 Then Return node
        EndIf
    Next
    Return Null
End Function

; Fonction pour sauvegarder les nodes
Function SaveNetwork(filename$)
    file = WriteFile(filename$)
    If file = 0 Then Return False
    
    count = 0
    For node.NodalNet = Each NodalNet
        count = count + 1
    Next
    WriteInt(file, count)
    
    For node.NodalNet = Each NodalNet
        WriteInt(file, node\NType)
        WriteInt(file, node\Radius)
        WriteFloat(file, node\Value)
        WriteInt(file, node\Px)
        WriteInt(file, node\Py)
        WriteString(file, node\Caption)
        parent_index = -1
        index = 0
        For n.NodalNet = Each NodalNet
            If n = node\Parent Then
                parent_index = index
                Exit
            EndIf
            index = index + 1
        Next
        WriteInt(file, parent_index)
    Next
    
    CloseFile(file)
    Return True
End Function

; Fonction pour charger les nodes
Function LoadNetwork(filename$)
    For node.NodalNet = Each NodalNet
        Delete node
    Next
    
    file = ReadFile(filename$)
    If file = 0 Then Return False
    
    count = ReadInt(file)
    
    For i = 1 To count
        node.NodalNet = New NodalNet
        node\NType = ReadInt(file)
        node\Radius = ReadInt(file)
        node\Value = ReadFloat(file)
        node\Px = ReadInt(file)
        node\Py = ReadInt(file)
        node\Caption = ReadString(file)
        node\TargetX = node\Px
        node\TargetY = node\Py
        ReadInt(file)
        last_selected = node ; Sélectionner le dernier node chargé
    Next
    
    SeekFile(file, 4)
    
    For node.NodalNet = Each NodalNet
        ReadInt(file)
        ReadInt(file)
        ReadFloat(file)
        ReadInt(file)
        ReadInt(file)
        ReadString(file)
        parent_index = ReadInt(file)
        
        If parent_index >= 0 Then
            index = 0
            For n.NodalNet = Each NodalNet
                If index = parent_index Then
                    node\Parent = n
                    Exit
                EndIf
                index = index + 1
            Next
        Else
            node\Parent = Null
        EndIf
    Next
    
    CloseFile(file)
    Return True
End Function

; Fonction principale
CreateTestNetwork()

While Not KeyHit(1)
    Cls
    
    ; Gestion du zoom avec la molette
    zoom_change = MouseZSpeed()
    If zoom_change > 0 Then cam_target_zoom = cam_target_zoom * 1.1
    If zoom_change < 0 Then cam_target_zoom = cam_target_zoom / 1.1
    If cam_target_zoom < 0.1 Then cam_target_zoom = 0.1
    
    If KeyHit(211) And last_selected <> Null Then
        DeleteNodeAndChildren(last_selected)
        last_selected = Null
    EndIf

    If MouseDown(1) Then
        If Not mouse_down Then
            mouse_down = True
            last_mx = MouseX()
            last_my = MouseY()
            selected_node = CheckNodeUnderMouse()
            If selected_node <> Null Then 
                last_selected = selected_node
            Else
                ; Si aucun node n'est sous la souris, désélectionner
                last_selected = Null
            EndIf
        Else
            If selected_node <> Null Then
                mx# = (MouseX() - 400) / cam_zoom - cam_x
                my# = (MouseY() - 300) / cam_zoom - cam_y
                selected_node\TargetX = mx
                selected_node\TargetY = my
            Else
                cam_target_x = cam_target_x + (MouseX() - last_mx) / cam_zoom
                cam_target_y = cam_target_y + (MouseY() - last_my) / cam_zoom
                last_mx = MouseX()
                last_my = MouseY()
            EndIf
        EndIf
    Else
        mouse_down = False
        selected_node = Null
    EndIf
    
    ; Bouton du milieu : création d'un root node avec connexion conditionnelle
    If MouseHit(2) Then
        new_root.NodalNet = New NodalNet
        new_root\NType = 0
        new_root\Px = (MouseX() - 400) / cam_zoom - cam_x
        new_root\Py = (MouseY() - 300) / cam_zoom - cam_y
        new_root\Caption = "Root" + Rnd(1,1000)
        new_root\TargetX = new_root\Px
        new_root\TargetY = new_root\Py
        
        ; Vérification corrigée pour éviter l'erreur
        If last_selected <> Null Then
            If last_selected\NType = 0 Then
                new_root\Parent = last_selected
            Else
                new_root\Parent = Null
            EndIf
        Else
            new_root\Parent = Null
        EndIf
        
        last_selected = new_root
    EndIf
    
    ; Bouton droit : création d'un node de valeur si un node est sélectionné
    If MouseHit(3) And last_selected <> Null Then
        new_node.NodalNet = New NodalNet
        new_node\NType = 1
        new_node\Value = Rnd(10,100)
        new_node\Radius = new_node\Value / 2
        new_node\Parent = last_selected
        new_node\Caption = "New" + Rnd(1,1000)
        new_node\Px = (MouseX() - 400) / cam_zoom - cam_x
        new_node\Py = (MouseY() - 300) / cam_zoom - cam_y
        new_node\TargetX = new_node\Px
        new_node\TargetY = new_node\Py

    ; Sélectionner automatiquement le dernier node créé
    last_selected = new_node

    EndIf
    
    ; Interpolation des positions pour les nodes
    For node.NodalNet = Each NodalNet
        If node\TargetX = 0 And node\TargetY = 0 Then
            node\TargetX = node\Px
            node\TargetY = node\Py
        EndIf
        node\Px = node\Px + (node\TargetX - node\Px) * 0.1
        node\Py = node\Py + (node\TargetY - node\Py) * 0.1
    Next
    
    ; Interpolation pour la caméra (position et zoom)
    cam_x = cam_x + (cam_target_x - cam_x) * 0.1
    cam_y = cam_y + (cam_target_y - cam_y) * 0.1
    cam_zoom = cam_zoom + (cam_target_zoom - cam_zoom) * 0.1
    
    ; Dessin du réseau
    For node.NodalNet = Each NodalNet
        screen_x# = (node\Px + cam_x) * cam_zoom + 400
        screen_y# = (node\Py + cam_y) * cam_zoom + 300
        
        If node\Parent <> Null Then
            parent_x# = (node\Parent\Px + cam_x) * cam_zoom + 400
            parent_y# = (node\Parent\Py + cam_y) * cam_zoom + 300
            Color 100,100,100
            Line screen_x, screen_y, parent_x, parent_y
        EndIf
        
        If node\NType = 0 Then
            Color 255,0,0
            Plot screen_x, screen_y
            Oval screen_x-3, screen_y-3, 6, 6, 0
            
            If node = selected_node Or node = last_selected Then
                Color 139,0,0
                radius_scaled# = 10 * cam_zoom
                Oval screen_x - radius_scaled, screen_y - radius_scaled, radius_scaled * 2, radius_scaled * 2, 0
            EndIf
            
            Color 255,255,255
            Text screen_x, screen_y+10, node\Caption, True, False
        Else
            radius_scaled# = node\Radius * cam_zoom
            If node = selected_node Then
                Color 0,255,0
            ElseIf node = last_selected Then
                Color 0,255,0
            ElseIf HasChildren(node) Then
                Color 255,165,0
            Else
                Color 0,0,255
            EndIf
            Oval screen_x - radius_scaled, screen_y - radius_scaled, radius_scaled * 2, radius_scaled * 2, 0
            Color 255,255,255
            Text screen_x, screen_y+20, node\Caption, True, False
            Text screen_x, screen_y, Int(node\Value), True, True
        EndIf
    Next
    
    ; Sauvegarde et chargement
    If KeyHit(31) Then SaveNetwork("network.txt") ; S
    If KeyHit(38) Then LoadNetwork("network.txt") ; L
    
    Color 255,255,255
    Text 10,10, "Zoom: "+cam_zoom
    Text 10,25, "Molette: zoom | Gauche: déplacer/sélectionner | Milieu: ajouter root | Droit: ajouter node"
    
    Flip
Wend

End

